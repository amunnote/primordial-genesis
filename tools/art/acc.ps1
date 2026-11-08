param([string]$OutDir,[string]$Sheet,[string]$Compare)
Add-Type -AssemblyName System.Drawing
$here=Split-Path -Parent $MyInvocation.MyCommand.Path
$strip={param($f)(Get-Content "$here\$f" -Raw) -replace '(?m)^using [^;]+;\r?\n',''}
$src=(Get-Content "$here\charms.cs" -Raw)+"`n"+(& $strip 'charm_designs.cs')+"`n"+(& $strip 'acc_designs.cs')
if(-not ('Designs' -as [type])){Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition $src -Language CSharp}
$names='amplify','armor_pierce','befuddlement','blood_pact','causality','curse','damage_cap','desaturation','enlightenment','growth','healing','magic','magic_resistance','malice','mastery','physical_resistance','reflection','regeneration','resistance','ruin','seal','shared','shatter','slowness','space','split','strength','summon','thunder','time','undying','void'|%{"${_}_accessory"}
$frames=8
if($OutDir){foreach($n in $names){$b=New-Object System.Drawing.Bitmap -ArgumentList 16,([int](16*$frames));for($f=0;$f -lt $frames;$f++){$fr=[Designs]::Draw($n,$f/$frames).Bmp();for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){$b.SetPixel($x,$y+16*$f,$fr.GetPixel($x,$y))}}};$b.Save("$OutDir\$n.png",[Drawing.Imaging.ImageFormat]::Png);[IO.File]::WriteAllText("$OutDir\$n.png.mcmeta",'{"animation":{"frametime":3}}',(New-Object Text.UTF8Encoding $false))}}
if($Sheet){$s=7;$cell=16*$s+6;$cols=16
  $canvas=New-Object System.Drawing.Bitmap -ArgumentList ([int]($cols*$cell)),([int](5*$cell));$g=[Drawing.Graphics]::FromImage($canvas);$g.Clear([Drawing.Color]::FromArgb(58,58,64));$g.InterpolationMode='NearestNeighbor';$g.PixelOffsetMode='Half'
  $i=0;foreach($n in $names){$g.DrawImage([Designs]::Draw($n,0).Bmp(),($i%$cols)*$cell,[Math]::Floor($i/$cols)*$cell,16*$s,16*$s);$i++}
  # a second frame of each, to see them move
  $i=0;foreach($n in $names){$g.DrawImage([Designs]::Draw($n,0.5).Bmp(),($i%$cols)*$cell,(2+[Math]::Floor($i/$cols))*$cell+10,16*$s,16*$s);$i++}
  # charms beside accessories, as they would sit together
  $j=0;foreach($n in 'basic_charm','breaking_edge_charm','law_of_arms_charm','unbreakable_charm','armor_pierce_accessory','blood_pact_accessory','thunder_accessory','time_accessory'){$g.DrawImage([Designs]::Draw($n,0).Bmp(),$j*$cell,4*$cell+20,16*$s,16*$s);$j++}
  $canvas.Save($Sheet)}
"done"