param([string]$OutDir,[string]$Sheet)
Add-Type -AssemblyName System.Drawing
$here=Split-Path -Parent $MyInvocation.MyCommand.Path
if(-not ('BossBars' -as [type])){Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition (Get-Content "$here\bossbars.cs" -Raw) -Language CSharp}
if($OutDir){New-Item -ItemType Directory -Force $OutDir | Out-Null
  foreach($t in [BossBars]::Themes){[BossBars]::Sheet($t).Save("$OutDir\$($t.Id).png",[Drawing.Imaging.ImageFormat]::Png)}}
if($Sheet){
  # each bar as the game draws it (client/BossBars) at GUI scale 3, its portrait ring empty (the game draws the boss in it)
  $s=3;$rowH=40;$canvas=New-Object System.Drawing.Bitmap -ArgumentList (240*$s),([int]([BossBars]::Themes.Count*$rowH*$s+12))
  $g=[Drawing.Graphics]::FromImage($canvas);$g.Clear([Drawing.Color]::FromArgb(110,150,200));$g.InterpolationMode='NearestNeighbor';$g.PixelOffsetMode='Half'
  function D($b,$gx,$gy,$u,$v,$w,$h){$g.DrawImage($b,(New-Object Drawing.Rectangle ($gx*$s),($gy*$s),($w*$s),($h*$s)),(New-Object Drawing.Rectangle $u,$v,$w,$h),'Pixel')}
  $i=0;foreach($t in [BossBars]::Themes){$b=[BossBars]::Sheet($t);$x=29;$y=$i*$rowH+27
    D $b ($x-21) ($y-11) 0 0 224 26; D $b $x $y 0 34 182 5; D $b $x $y 0 28 127 5
    D $b ($x+78) ($y-26) 28 40 26 26; D $b ($x+78) ($y-26) 0 40 26 26
    $i++}
  $canvas.Save($Sheet)}
"done"
