param([string]$Assets,[string]$Sheet)
Add-Type -AssemblyName System.Drawing
$here=Split-Path -Parent $MyInvocation.MyCommand.Path
$strip={param($f)(Get-Content "$here\$f" -Raw) -replace '(?m)^using [^;]+;\r?\n',''}
$src=(Get-Content "$here\charms.cs" -Raw)+"`n"+(& $strip 'charm_designs.cs')+"`n"+(& $strip 'acc_designs.cs')+"`n"+(& $strip 'recipe_designs.cs')+"`n"+(& $strip 'fauna_items.cs')
if(-not ('Designs' -as [type])){Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition $src -Language CSharp}
$items='bifang_flame_crystal','thunder_firefly_bottle','golden_bell_feather','whistling_arrow','netherlamp_sac','netherlamp','netherlamp_fish_bucket'
if($Assets){foreach($n in $items){[Designs]::Draw("fauna_$n",0).Bmp().Save("$Assets\textures\item\$n.png",[Drawing.Imaging.ImageFormat]::Png)}
  [Designs]::LanternTexture($true).Save("$Assets\textures\block\thunder_firefly_jar.png",[Drawing.Imaging.ImageFormat]::Png)
  [Designs]::LanternTexture($false).Save("$Assets\textures\block\netherlamp.png",[Drawing.Imaging.ImageFormat]::Png)}
if($Sheet){$s=8;$cell=16*$s+8;$canvas=New-Object System.Drawing.Bitmap -ArgumentList ([int](9*$cell)),([int]$cell);$g=[Drawing.Graphics]::FromImage($canvas);$g.Clear([Drawing.Color]::FromArgb(58,58,64));$g.InterpolationMode='NearestNeighbor';$g.PixelOffsetMode='Half'
  $i=0;foreach($n in $items){$g.DrawImage([Designs]::Draw("fauna_$n",0).Bmp(),$i*$cell,0,16*$s,16*$s);$i++}
  $g.DrawImage([Designs]::LanternTexture($true),$i*$cell,0,16*$s,16*$s);$i++;$g.DrawImage([Designs]::LanternTexture($false),$i*$cell,0,16*$s,16*$s);$canvas.Save($Sheet)}
"done"