param([string]$OutDir,[string]$Sheet)
Add-Type -AssemblyName System.Drawing
$here=Split-Path -Parent $MyInvocation.MyCommand.Path
$strip={param($f)(Get-Content "$here\$f" -Raw) -replace '(?m)^using [^;]+;\r?\n',''}
$src=(Get-Content "$here\charms.cs" -Raw)+"`n"+(& $strip 'charm_designs.cs')+"`n"+(& $strip 'acc_designs.cs')+"`n"+(& $strip 'recipe_designs.cs')+"`n"+(& $strip 'fauna_items.cs')+"`n"+(& $strip 'capsule_designs.cs')
if(-not ('Designs' -as [type])){Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition $src -Language CSharp}
$names=foreach($i in 'alchemy_recipe','recipe_fragment','mystery_recipe'){foreach($t in 1..4){"${i}_tier$t"}}
function Frame($n,$p){return [Designs]::Draw($n,$p).Bmp()}
if($OutDir){foreach($n in $names){
  if($n -eq 'alchemy_recipe_tier4'){$b=New-Object System.Drawing.Bitmap -ArgumentList 16,128;for($f=0;$f -lt 8;$f++){$fr=Frame $n ($f/8);for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){$b.SetPixel($x,$y+16*$f,$fr.GetPixel($x,$y))}}};$b.Save("$OutDir\$n.png",[Drawing.Imaging.ImageFormat]::Png);[IO.File]::WriteAllText("$OutDir\$n.png.mcmeta",'{"animation":{"frametime":3}}',(New-Object Text.UTF8Encoding $false))}
  else{(Frame $n 0).Save("$OutDir\$n.png",[Drawing.Imaging.ImageFormat]::Png)}}}
if($Sheet){$s=8;$cell=16*$s+8;$canvas=New-Object System.Drawing.Bitmap -ArgumentList ([int](4*$cell)),([int](3*$cell));$g=[Drawing.Graphics]::FromImage($canvas);$g.Clear([Drawing.Color]::FromArgb(58,58,64));$g.InterpolationMode='NearestNeighbor';$g.PixelOffsetMode='Half'
  $i=0;foreach($n in $names){$g.DrawImage((Frame $n 0),($i%4)*$cell,[Math]::Floor($i/4)*$cell,16*$s,16*$s);$i++};$canvas.Save($Sheet)}
"done"