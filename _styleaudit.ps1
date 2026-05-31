$ErrorActionPreference = 'Stop'
$root = 'C:\Users\Pheas\Documents\GitHub\Exploding_Kittens'
$viewDir = Join-Path $root 'src\main\resources\view'
$javaDir = Join-Path $root 'src\main\java'
$cssDir  = Join-Path $root 'src\main\resources\css'

# token -> info object
$tokens = @{}

function Add-Usage([string]$tok, [string]$ctx, [string]$where, [bool]$isControl) {
    if ([string]::IsNullOrWhiteSpace($tok)) { return }
    $tok = $tok.Trim()
    if ($tok -eq '') { return }
    if (-not $tokens.ContainsKey($tok)) {
        $tokens[$tok] = [pscustomobject]@{
            Token = $tok
            FirstWhere = $where
            Contexts = New-Object System.Collections.Generic.HashSet[string]
            IsControl = $false
        }
    }
    $null = $tokens[$tok].Contexts.Add($ctx)
    if ($isControl) { $tokens[$tok].IsControl = $true }
    if (-not $tokens[$tok].FirstWhere) { $tokens[$tok].FirstWhere = $where }
}

# Control tags / variable-name heuristics for interactive controls
$controlTags = @('Button','ToggleButton','MenuButton','SplitMenuButton','RadioButton','CheckBox',
    'ComboBox','ChoiceBox','TextField','PasswordField','TextArea','DatePicker','Slider','Spinner',
    'ColorPicker','Hyperlink','ToggleSwitch','SearchField','ListView','TableView','TreeView','TreeTableView')

$controlVarRegex = '(?i)(button|btn|toggle|checkbox|radio|combo|choice|field|textarea|datepicker|picker|slider|spinner|hyperlink|switch|menubtn|menubutton)'

# ---------- FXML ----------
$fxmlFiles = Get-ChildItem -Path $viewDir -Recurse -Filter *.fxml
foreach ($f in $fxmlFiles) {
    $lines = Get-Content -LiteralPath $f.FullName
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        $m = [regex]::Match($line, 'styleClass\s*=\s*"([^"]*)"')
        if (-not $m.Success) { continue }
        $value = $m.Groups[1].Value
        # find the element tag: search backwards from this line for the most recent '<Tag'
        $tag = $null
        for ($j = $i; $j -ge 0 -and $j -ge ($i-12); $j--) {
            $tm = [regex]::Match($lines[$j], '<([A-Za-z][A-Za-z0-9]*)')
            if ($tm.Success) {
                # take the LAST opening tag on/before this line that is at or before our position
                $allTags = [regex]::Matches($lines[$j], '<([A-Za-z][A-Za-z0-9]*)')
                $tag = $allTags[$allTags.Count-1].Groups[1].Value
                break
            }
        }
        $isCtrl = $false
        if ($tag -and ($controlTags -contains $tag)) { $isCtrl = $true }
        $where = "{0}:{1}" -f $f.FullName.Substring($root.Length+1), ($i+1)
        $ctx = "FXML<$tag>"
        foreach ($t in ($value -split '\s+')) {
            Add-Usage $t $ctx $where $isCtrl
        }
    }
}

# ---------- JAVA ----------
$javaFiles = Get-ChildItem -Path $javaDir -Recurse -Filter *.java
foreach ($f in $javaFiles) {
    $lines = Get-Content -LiteralPath $f.FullName
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        # match receiver.getStyleClass().add( ... ) or addAll( ... )  possibly spanning, but grab this line + next 2
        $m = [regex]::Match($line, '([A-Za-z_][A-Za-z0-9_]*)\s*\.\s*getStyleClass\(\)\s*\.\s*(add|addAll)\s*\(')
        if (-not $m.Success) { continue }
        $recv = $m.Groups[1].Value
        # gather the argument text (this line from the '(' plus up to 3 following lines, until balanced ')')
        $startIdx = $m.Index + $m.Length
        $buf = $line.Substring($startIdx)
        $k = $i
        while ($buf -notmatch '\)' -and $k -lt [Math]::Min($i+4, $lines.Count-1)) {
            $k++
            $buf += ' ' + $lines[$k]
        }
        # extract up to the first ')'
        $endParen = $buf.IndexOf(')')
        if ($endParen -ge 0) { $buf = $buf.Substring(0, $endParen) }
        # find all string literals
        $strs = [regex]::Matches($buf, '"([^"]*)"')
        $isCtrl = $false
        if ($recv -match $controlVarRegex) { $isCtrl = $true }
        $where = "{0}:{1}" -f $f.FullName.Substring($root.Length+1), ($i+1)
        $ctx = "JAVA:$recv"
        foreach ($s in $strs) {
            $lit = $s.Groups[1].Value
            # skip literals that are obviously concatenated/dynamic fragments containing no usefulness? keep them
            Add-Usage $lit $ctx $where $isCtrl
        }
    }
}

# ---------- CSS analysis ----------
# Build, per css file, a map token -> set of properties present in any rule whose selector chain contains .token
$cssFiles = Get-ChildItem -Path $cssDir -Recurse -Filter *.css
# We'll parse each css into (selectorText, bodyText) rule blocks.
$cssRules = @{}  # file -> list of @{Sel=...; Body=...}
foreach ($c in $cssFiles) {
    $text = Get-Content -LiteralPath $c.FullName -Raw
    # strip comments
    $text = [regex]::Replace($text, '/\*.*?\*/', ' ', 'Singleline')
    $rules = New-Object System.Collections.Generic.List[object]
    $rx = [regex]::Matches($text, '([^{}]+)\{([^{}]*)\}')
    foreach ($r in $rx) {
        $rules.Add([pscustomobject]@{ Sel = $r.Groups[1].Value; Body = $r.Groups[2].Value })
    }
    $cssRules[$c.Name] = $rules
}

function Get-TokenCss([string]$tok) {
    # returns object: HasBg, HasTextFill, FilesWithVisual(list), FilesWithAny(list), TypographyOnly
    $bg = $false; $tf = $false
    $filesVisual = New-Object System.Collections.Generic.HashSet[string]
    $filesAny = New-Object System.Collections.Generic.HashSet[string]
    $esc = [regex]::Escape($tok)
    # selector must contain .tok as a whole class token (followed by non class-char)
    $selPat = "\.$esc(?![A-Za-z0-9_-])"
    foreach ($fname in $cssRules.Keys) {
        foreach ($rule in $cssRules[$fname]) {
            if ($rule.Sel -match $selPat) {
                $null = $filesAny.Add($fname)
                $body = $rule.Body
                $hasBg = $body -match '-fx-background-color\s*:'
                $hasTf = $body -match '-fx-text-fill\s*:'
                if ($hasBg) { $bg = $true }
                if ($hasTf) { $tf = $true }
                if ($hasBg -or $hasTf) { $null = $filesVisual.Add($fname) }
            }
        }
    }
    $typographyOnly = ($filesAny.Count -gt 0) -and ($filesVisual.Count -eq 0)
    [pscustomobject]@{
        HasBg = $bg
        HasTextFill = $tf
        HasVisual = ($bg -or $tf)
        FilesAny = ($filesAny -join ',')
        FilesVisual = ($filesVisual -join ',')
        AnyCount = $filesAny.Count
        TypographyOnly = $typographyOnly
    }
}

$results = New-Object System.Collections.Generic.List[object]
foreach ($tk in $tokens.Keys) {
    # skip dynamic concatenation fragments (contain no letters or look like partial)
    $info = $tokens[$tk]
    $css = Get-TokenCss $tk
    $results.Add([pscustomobject]@{
        Token = $tk
        IsControl = $info.IsControl
        Where = $info.FirstWhere
        Contexts = ($info.Contexts -join '; ')
        HasVisual = $css.HasVisual
        AnyCss = ($css.AnyCount -gt 0)
        TypographyOnly = $css.TypographyOnly
        CssFilesAny = $css.FilesAny
        CssFilesVisual = $css.FilesVisual
    })
}

# Output: focus on controls lacking visual styling
Write-Host "===== TOTAL UNIQUE TOKENS:" $results.Count "====="
Write-Host ""
Write-Host "########## CONTROL TOKENS WITH NO VISUAL CSS ##########"
$bad = $results | Where-Object { $_.IsControl -and -not $_.HasVisual } | Sort-Object Token
foreach ($b in $bad) {
    $cat = if (-not $b.AnyCss) { 'NO-CSS-AT-ALL' } elseif ($b.TypographyOnly) { 'TYPOGRAPHY-ONLY' } else { 'OTHER-NONVISUAL' }
    Write-Host ("[{0}] {1}" -f $cat, $b.Token)
    Write-Host ("      where: {0}" -f $b.Where)
    Write-Host ("      ctx:   {0}" -f $b.Contexts)
    Write-Host ("      cssAny: {0}" -f $b.CssFilesAny)
}
Write-Host ""
Write-Host "########## (REFERENCE) ALL NON-VISUAL TOKENS regardless of control flag ##########"
$allbad = $results | Where-Object { -not $_.HasVisual } | Sort-Object Token
Write-Host ("Count: {0}" -f $allbad.Count)

# Save full CSV for deeper inspection
$results | Sort-Object Token | Export-Csv -LiteralPath (Join-Path $root '_styleaudit.csv') -NoTypeInformation -Encoding utf8
Write-Host "CSV written."
