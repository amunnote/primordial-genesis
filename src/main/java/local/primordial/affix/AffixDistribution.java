package local.primordial.affix;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Random;

public final class AffixDistribution {
    private AffixDistribution() {}
    public static int maximum(int level) {
        // No arbitrary affix ceiling: selection only stops when the distinct pool is exhausted.
        return Math.min(MobAffix.values().length, 2+Math.max(0,level)/20);
    }
    /** Index is the exact number of distinct ordinary affixes; every eligible mob has at least one. */
    public static double[] probabilities(int level) {
        int maximum=maximum(level);double[] probabilities=new double[maximum+1];double extra=0;
        for(int count=2;count<=maximum;count++)extra+=(maximum-count+1)*0.1;
        if(extra>1){
            for(int count=2;count<=maximum;count++)probabilities[count]=(maximum-count+1)*0.1/extra;
        }else{
            for(int count=2;count<=maximum;count++)probabilities[count]=(maximum-count+1)*0.1;
            probabilities[1]=1-extra;
        }
        return probabilities;
    }
    public static int count(int level,double roll,boolean boss) {
        var probabilities=probabilities(level);double cumulative=0;
        int count=probabilities.length-1;
        for(int i=0;i<probabilities.length;i++){cumulative+=probabilities[i];if(roll<cumulative){count=i;break;}}
        return boss?Math.max(5,count):Math.max(1,count);
    }
    public static EnumSet<MobAffix> choose(int count,long seed) {return choose(count,seed,affix->true);}
    /**
     * Up to count distinct affixes the mob can use; fewer if the usable pool runs out. The first is drawn evenly; each
     * after it is weighted toward the affixes that work with those already chosen (see {@link AffixSynergy}).
     */
    public static EnumSet<MobAffix> choose(int count,long seed,java.util.function.Predicate<MobAffix> usable) {
        var random=new Random(seed);
        var selected=EnumSet.noneOf(MobAffix.class);
        while(selected.size()<count){
            var pool=new ArrayList<MobAffix>();
            for(var affix:MobAffix.values())if(!selected.contains(affix)&&usable.test(affix)&&!AffixEligibility.conflicts(affix,selected))pool.add(affix);
            if(pool.isEmpty())break;
            int total=0;for(var affix:pool)total+=AffixSynergy.weight(affix,selected);
            int pick=random.nextInt(total);
            for(var affix:pool){pick-=AffixSynergy.weight(affix,selected);if(pick<0){selected.add(affix);break;}}
        }
        return selected;
    }
}
