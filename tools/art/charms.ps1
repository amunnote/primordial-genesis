param([string]$OutDir,[string]$Sheet)
Add-Type -AssemblyName System.Drawing
$here=Split-Path -Parent $MyInvocation.MyCommand.Path
$src=(Get-Content "$here\charms.cs" -Raw)+"`n"+((Get-Content "$here\charm_designs.cs" -Raw) -replace '(?m)^using [^;]+;\r?\n','')+"`n"+((Get-Content "$here\acc_designs.cs" -Raw) -replace '(?m)^using [^;]+;\r?\n','')
if(-not ('Designs' -as [type])){Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition $src -Language CSharp}
$static='basic_charm','breaking_edge_charm','conquest_charm','spellbreaker_charm','arcane_pact_charm','hundred_forged_charm','bulwark_pact_charm','crystal_vestment_charm','astral_pact_charm','thousand_blade_charm','swift_pact_charm','quickstring_charm','swiftstring_pact_charm'
# animated: name -> frames, frame time (ticks)
$anim=[ordered]@{law_of_arms_charm=@(8,3);law_of_arcana_charm=@(8,3);law_of_gale_charm=@(8,2);law_of_tailwind_charm=@(8,2);unyielding_body_charm=@(8,3);arcane_harmony_charm=@(8,3);
 omni_amplify_charm=@(12,2);fleeting_eternity_charm=@(12,3);unbreakable_charm=@(12,2);spell_annul_charm=@(12,2);clear_world_charm=@(16,2);inviolable_charm=@(16,2)}
function Strip($name,$frames){$n=[Cv]::N;$b=New-Object System.Drawing.Bitmap -ArgumentList $n,([int]($n*$frames));for($f=0;$f -lt $frames;$f++){$fr=[Designs]::Draw($name,$f/$frames).Bmp();for($x=0;$x -lt $n;$x++){for($y=0;$y -lt $n;$y++){$b.SetPixel($x,$y+$n*$f,$fr.GetPixel($x,$y))}}};return $b}
if($OutDir){
  foreach($n in $static){[Designs]::Draw($n,0).Bmp().Save("$OutDir\$n.png",[Drawing.Imaging.ImageFormat]::Png);if(Test-Path "$OutDir\$n.png.mcmeta"){Remove-Item "$OutDir\$n.png.mcmeta"}}
  foreach($n in $anim.Keys){(Strip $n $anim[$n][0]).Save("$OutDir\$n.png",[Drawing.Imaging.ImageFormat]::Png);[IO.File]::WriteAllText("$OutDir\$n.png.mcmeta","{`"animation`":{`"frametime`":$($anim[$n][1])}}",(New-Object Text.UTF8Encoding $false))}
}
if($Sheet){
  $s=8;$cell=16*$s+8;$cols=13;$rows=2+[Math]::Ceiling($anim.Count)
  $canvas=New-Object System.Drawing.Bitmap -ArgumentList ([int]($cols*$cell+40)),([int]((2+$anim.Count/3)*$cell))
  $g=[Drawing.Graphics]::FromImage($canvas);$g.Clear([Drawing.Color]::FromArgb(58,58,64));$g.InterpolationMode='NearestNeighbor';$g.PixelOffsetMode='Half'
  $i=0;foreach($n in $static){$g.DrawImage([Designs]::Draw($n,0).Bmp(),($i%$cols)*$cell,[Math]::Floor($i/$cols)*$cell,16*$s,16*$s);$i++}
  # animated: four frames each, three charms to a row
  $r=2;$k=0;foreach($n in $anim.Keys){for($f=0;$f -lt 4;$f++){$g.DrawImage([Designs]::Draw($n,$f/4).Bmp(),(($k%3)*4+$f)*$cell+($k%3)*4,($r+[Math]::Floor($k/3))*$cell,16*$s,16*$s)};$k++}
  $canvas.Save($Sheet)
}
"done"
