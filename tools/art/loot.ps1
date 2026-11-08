param([string]$OutDir,[string]$Sheet)
Add-Type -AssemblyName System.Drawing
$here=Split-Path -Parent $MyInvocation.MyCommand.Path
$strip={param($f)(Get-Content "$here\$f" -Raw) -replace '(?m)^using [^;]+;\r?\n',''}
$src=(Get-Content "$here\charms.cs" -Raw)+"`n"+(& $strip 'charm_designs.cs')+"`n"+(& $strip 'acc_designs.cs')+"`n"+(& $strip 'recipe_designs.cs')+"`n"+(& $strip 'fauna_items.cs')+"`n"+(& $strip 'capsule_designs.cs')+"`n"+(& $strip 'loot_items.cs')
if(-not ('Designs' -as [type])){Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition $src -Language CSharp}
# the two charms are scrolls of eight frames; the rest still
$charms='worn_traveler_charm','iron_vitality_charm'
$items='substitute_doll','torn_recipe_page','glowstone_earring','apprentice_parcel'
if($OutDir){
  foreach($n in $charms){$b=New-Object System.Drawing.Bitmap -ArgumentList 16,128;for($i=0;$i -lt 8;$i++){$fr=[Designs]::Scroll($n,$i/8).Bmp();for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){$b.SetPixel($x,$y+16*$i,$fr.GetPixel($x,$y))}}}
    $b.Save("$OutDir\$n.png",[Drawing.Imaging.ImageFormat]::Png);[IO.File]::WriteAllText("$OutDir\$n.png.mcmeta",'{"animation":{"frametime":3}}',(New-Object Text.UTF8Encoding $false))}
  foreach($n in $items){[Designs]::Draw("loot_$n",0).Bmp().Save("$OutDir\$n.png",[Drawing.Imaging.ImageFormat]::Png)}
}
if($Sheet){$s=8;$cell=16*$s+8;$canvas=New-Object System.Drawing.Bitmap -ArgumentList ([int](6*$cell)),([int]$cell);$g=[Drawing.Graphics]::FromImage($canvas);$g.Clear([Drawing.Color]::FromArgb(139,139,139));$g.InterpolationMode='NearestNeighbor';$g.PixelOffsetMode='Half'
  $i=0;foreach($n in $charms){$g.DrawImage([Designs]::Scroll($n,0.3).Bmp(),$i*$cell,0,16*$s,16*$s);$i++}
  foreach($n in $items){$g.DrawImage([Designs]::Draw("loot_$n",0).Bmp(),$i*$cell,0,16*$s,16*$s);$i++}
  $canvas.Save($Sheet)}
"done"
