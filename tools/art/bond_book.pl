use strict;use utf8;use JSON::PP;
binmode STDOUT,':utf8';
my ($src,$res)=@ARGV;  # src/main  src/main/resources
open my $f,'<:utf8',"$src/java/local/primordial/affix/AffixBonds.java" or die;my $java=do{local $/;<$f>};
my @bonds;
while($java=~/^\s+([A-Z_]+)\("(\w+)",0xFF([0-9A-F]{6}),((?:MobAffix\.[A-Z_]+,?)+)\)/mg){my($id,$col,$parts)=($2,$3,$4);push @bonds,[$id,$col,[map{lc}($parts=~/MobAffix\.([A-Z_]+)/g)]];}
die "no bonds" unless @bonds==20;
for my $lang('zh_cn','en_us'){
  open my $l,'<:raw',"$res/assets/primordial/lang/$lang.json" or die;my $t=decode_json(do{local $/;<$l>});
  my $zh=$lang eq 'zh_cn';
  my @pages=({type=>'patchouli:text',title=>$zh?'羁绊':'Bonds',text=>$zh
    ?'同时佩戴特定的几件词条饰品，就会激活它们之间的$(l)羁绊$()，获得额外效果。$(br2)$(li)计入的是饰品栏中的饰品，以及主手武器上锻入的词条$(li)与顺序、栏位无关，凑齐所有部件即生效，取下任意一件即失效$(li)可以同时激活任意多个羁绊$(li)按住 Shift 查看饰品的羁绊进度；状态界面列出当前激活的羁绊$(li)羁绊是独立状态：牛奶清除不了，贤者之赐也不会改变它'
    :'Wear certain affix accessories together and the $(l)bond$() between them holds, with an effect of its own.$(br2)$(li)What counts: accessories in real slots, and the affixes forged into your main-hand weapon$(li)Order and slot do not matter: every part worn and it holds; take one off and it breaks$(li)Any number of bonds can hold at once$(li)Hold Shift on an accessory to see its bonds; the status screen lists the ones holding$(li)A bond is its own status: milk does not clear it, and Sage\'s Gift does not change it'});
  for my $b(@bonds){my($id,$col,$parts)=@$b;
    my $name=$t->{"bond.primordial.$id"} or die "$lang $id";my $desc=$t->{"bond.primordial.$id.desc"} or die;
    my @pn=map{my $k="item.primordial.${_}_accessory";$t->{$k} or die "$k"}@$parts;
    my $sep=$zh?'、':', ';
    push @pages,{type=>"patchouli:text",text=>"\$(#$col)\$(l)$name\$()\$(br)".($zh?'部件：':'Parts: ').join($sep,@pn)."\$(br2)$desc"};
  }
  my $entry={name=>$zh?'羁绊':'Bonds',icon=>'primordial:causality_accessory',category=>'primordial:accessories',sortnum=>5,pages=>\@pages};
  open my $o,'>:raw',"$res/assets/primordial/patchouli_books/alchemist_gift/$lang/entries/accessories/bonds.json" or die;
  print $o JSON::PP->new->utf8->pretty->canonical->encode($entry);
  print "$lang ".scalar(@pages)." pages\n";
}
