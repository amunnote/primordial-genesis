param([string]$Assets)
Add-Type -AssemblyName System.Drawing
$here=Split-Path -Parent $MyInvocation.MyCommand.Path
$src=(Get-Content "$here\creatures.cs" -Raw)+"`n"+((Get-Content "$here\creature_designs.cs" -Raw) -replace '(?m)^using [^;]+;\r?\n','')
if(-not ('CreatureDesigns' -as [type])){Add-Type -ReferencedAssemblies System.Drawing,System.Core -TypeDefinition $src -Language CSharp}
foreach($c in [CreatureDesigns]::Bifang(),[CreatureDesigns]::Firefly(),[CreatureDesigns]::Sparrow(),[CreatureDesigns]::Lampfish()){$c.Write($Assets);"wrote $($c.Id)"}