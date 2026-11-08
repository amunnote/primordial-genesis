param([string]$OutDir,[string]$Sheet)
Add-Type -AssemblyName System.Drawing
$here=Split-Path -Parent $MyInvocation.MyCommand.Path
$strip={param($f)(Get-Content "$here\$f" -Raw) -replace '(?m)^using [^;]+;\r?\n',''}
$src=(Get-Content "$here\charms.cs" -Raw)+"`n"+(& $strip 'charm_designs.cs')+"`n"+(& $strip 'acc_designs.cs')+"`n"+(& $strip 'recipe_designs.cs')+"`n"+(& $strip 'fauna_items.cs')+"`n"+(& $strip 'capsule_designs.cs')
if(-not ('Designs' -as [type])){Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition $src -Language CSharp}
$names='basic_charm','breaking_edge_charm','conquest_charm','spellbreaker_charm','arcane_pact_charm','hundred_forged_charm','bulwark_pact_charm','crystal_vestment_charm','astral_pact_charm','thousand_blade_charm','swift_pact_charm','quickstring_charm','swiftstring_pact_charm','law_of_arms_charm','law_of_arcana_charm','law_of_gale_charm','law_of_tailwind_charm','unyielding_body_charm','arcane_harmony_charm','omni_amplify_charm','fleeting_eternity_charm','unbreakable_charm','spell_annul_charm','clear_world_charm','inviolable_charm'
$frames=@{law_of_arms_charm=8;law_of_arcana_charm=8;law_of_gale_charm=8;law_of_tailwind_charm=8;unyielding_body_charm=8;arcane_harmony_charm=8;omni_amplify_charm=12;fleeting_eternity_charm=12;unbreakable_charm=12;spell_annul_charm=12;clear_world_charm=16;inviolable_charm=16}
if($OutDir){foreach($n in $names){$f=$frames[$n];if(-not $f){$f=8}
  $b=New-Object System.Drawing.Bitmap -ArgumentList 16,([int](16*$f));for($i=0;$i -lt $f;$i++){$fr=[Designs]::Scroll($n,$i/$f).Bmp();for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){$b.SetPixel($x,$y+16*$i,$fr.GetPixel($x,$y))}}}
  $b.Save("$OutDir\$n.png",[Drawing.Imaging.ImageFormat]::Png);[IO.File]::WriteAllText("$OutDir\$n.png.mcmeta","{`"animation`":{`"frametime`":$(if($f -ge 12){2}else{3})}}",(New-Object Text.UTF8Encoding $false))}}
if($Sheet){$s=7;$cell=16*$s+6;$cols=13;$canvas=New-Object System.Drawing.Bitmap -ArgumentList ([int]($cols*$cell)),([int](3*$cell));$g=[Drawing.Graphics]::FromImage($canvas);$g.Clear([Drawing.Color]::FromArgb(58,58,64));$g.InterpolationMode='NearestNeighbor';$g.PixelOffsetMode='Half'
  $i=0;foreach($n in $names){$g.DrawImage([Designs]::Scroll($n,0.35).Bmp(),($i%$cols)*$cell,[Math]::Floor($i/$cols)*$cell,16*$s,16*$s);$i++}
  $canvas.Save($Sheet)}
"done"