import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class JackCompiler {

    private final Tokenizer tokenizer;
    private final Parser parser;

    private JackCompiler(Tokenizer tokenizer,Parser parser){
        this.tokenizer = tokenizer;
        this.parser = parser;
    }

    public void compile() throws Exception {
        tokenizer.readLines();
        parser.readTokens();
    }

    public static void start(String args) throws Exception {
        File file = new File(args);
        // dir compilation
        if(file.isDirectory()){
            List<File> files = Arrays.stream(Objects.requireNonNull(file.listFiles()))
                    .filter(x -> Pattern.compile("(.jack)").matcher(x.getName()).find()).toList();
            for(File f : files){
                // file is dir
                creation(f,file);
            }
        }
        // single file should get compiled
        else if(file.getName().endsWith(".jack")){
            String parent = file.getParent();
            if(parent == null) creation(file,null);
            else creation(file,Path.of(parent).toFile());
        }
        // target is not supported
        else{
            throw new Exception("Target Not Supported!");
        }
        // remove all files that are not needed anymore -> .xml
        clearFiles();
    }

    private static void clearFiles(){
        new File("Main.xml").delete();
        new File("MainT.xml").delete();
    }

    private static void creation(File f, File dir) throws Exception {
        String s;
        if(dir == null) s = "";
        else s = dir.getName();
        Tokenizer tokenizer;
        FileOutputStream fileOutputStream;
        VMCodeGenerator vmCodeGenerator;
        BufferedInputStream bufferedInputStream;
        JackCompiler jackCompiler;
        bufferedInputStream = new BufferedInputStream(new FileInputStream(f));
        fileOutputStream = new FileOutputStream("MainT.xml");
        tokenizer = new Tokenizer(bufferedInputStream,fileOutputStream);
        bufferedInputStream = new BufferedInputStream(new FileInputStream("MainT.xml"));
        fileOutputStream = new FileOutputStream("Main.xml");
        vmCodeGenerator = new VMCodeGenerator(new FileOutputStream(fileNameCreation(s,f.getName())));
        Parser parser = new Parser(bufferedInputStream,fileOutputStream,vmCodeGenerator, new ArrayList<>());
        jackCompiler = new JackCompiler(tokenizer,parser);
        jackCompiler.compile();
    }

    private static String fileNameCreation(String dir,String currentFileName){
        if(dir == null) return currentFileName;
        String s = currentFileName.split("(.jack)")[0];
        return Path.of(String.format("%s/%s.vm",dir,s)).toString();
    }

    public static void main(String[] args) throws Exception {
        start(args[0]);
    }
}
