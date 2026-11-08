import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

/** Rewrites gzip NBT files in place: every string and compound key gets the angel_charm -> primordial id rename. */
public class NbtRename {
    static int changed;
    static String fix(String s){
        String t=s.replace("angel_charm:","primordial:").replaceAll("\\.angel_charm(?=[.\"\\]])",".primordial");
        if(!t.equals(s))changed++;
        return t;
    }
    static void copy(int type,DataInputStream in,DataOutputStream out) throws IOException {
        switch(type){
            case 0->{}
            case 1->out.writeByte(in.readByte());
            case 2->out.writeShort(in.readShort());
            case 3->out.writeInt(in.readInt());
            case 4->out.writeLong(in.readLong());
            case 5->out.writeFloat(in.readFloat());
            case 6->out.writeDouble(in.readDouble());
            case 7->{int n=in.readInt();out.writeInt(n);byte[] b=new byte[n];in.readFully(b);out.write(b);}
            case 8->out.writeUTF(fix(in.readUTF()));
            case 9->{int t=in.readByte();int n=in.readInt();out.writeByte(t);out.writeInt(n);for(int i=0;i<n;i++)copy(t,in,out);}
            case 10->{while(true){int t=in.readByte();out.writeByte(t);if(t==0)break;out.writeUTF(fix(in.readUTF()));copy(t,in,out);}}
            case 11->{int n=in.readInt();out.writeInt(n);for(int i=0;i<n;i++)out.writeInt(in.readInt());}
            case 12->{int n=in.readInt();out.writeInt(n);for(int i=0;i<n;i++)out.writeLong(in.readLong());}
            default->throw new IOException("unknown tag "+type);
        }
    }
    public static void main(String[] args) throws Exception {
        for(String a:args)try(var files=Files.walk(Path.of(a))){
            for(Path p:(Iterable<Path>)files.filter(f->f.toString().endsWith(".nbt"))::iterator){
                changed=0;var bytes=new ByteArrayOutputStream();
                try(var in=new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(p))));var out=new DataOutputStream(bytes)){
                    int t=in.readByte();out.writeByte(t);out.writeUTF(fix(in.readUTF()));copy(t,in,out);
                }
                if(changed>0){try(var gz=new GZIPOutputStream(Files.newOutputStream(p))){gz.write(bytes.toByteArray());}}
                System.out.println(p+" "+changed);
            }
        }
    }
}
