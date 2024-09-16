import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class TestCompiler {

    public static boolean compare(File in, File cmp, boolean active) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(in));
        BufferedReader bufferedReader1 = new BufferedReader(new FileReader(cmp));
        List<String> inputLines = bufferedReader.lines().collect(Collectors.toList());
        List<String> cmpLines = bufferedReader1.lines().toList();
        if(inputLines.size() != cmpLines.size()){
            System.out.println("Not same length !");
            System.out.println("Expected size : " + cmpLines.size() + " ,but was : " + inputLines.size());
        }
        if(active){
            inputLines = inputLines.stream().map(String::strip).toList();
            cmpLines = cmpLines.stream().map(String::strip).toList();
        }
        boolean j = true;
        for(int i = 0; i < cmpLines.size(); i++){
            try{
                if(!(inputLines.get(i).equals(cmpLines.get(i)))){
                    j = false;
                    System.out.println("Expected : " + cmpLines.get(i) + " ,but was : " + inputLines.get(i));
                }
            }
            catch (IndexOutOfBoundsException indexOutOfBoundsException){
                break;
            }
        }
        System.out.println("----------------");
        return j;
    }

    public static void main(String[] args) throws IOException {
        //only for part compiler 1
        System.out.println(compare(Path.of("MainT.xml").toFile(),Path.of(args[1]).toFile(),false));
        System.out.println(compare(Path.of("Main.xml").toFile(),
                Path.of(args[0]).toFile(),true));
    }
}
