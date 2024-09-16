import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class Translator {
    FileOutputStream fileOutputStream;
    Map<String,String> mapSegments = new HashMap<>();
    Map<String,List<Integer>> staticMap = new HashMap<>();
    Stack<String> classNames = new Stack<>();
    Integer STATIC_COUNTER = 0;
    Integer POINTER_LABEL = 0;
    Integer COMPARE_LABEL = 0;
    Integer FUNCTION_RETURN = 1;
    String TAB_GENERATOR = "\t";

    public Translator(File file) throws IOException {
        file.delete();
        file.createNewFile();
        fileOutputStream = new FileOutputStream(file,true);
        segmentPointer();
    }

    private void segmentPointer(){
        mapSegments.put("local","@LCL");
        mapSegments.put("argument","@ARG");
        mapSegments.put("this","@THIS");
        mapSegments.put("that","@THAT");
    }

    // add(1) or push local 0(3)
    private boolean validLength(String s){
        String[] split = s.split(" ");
        return split.length <= 3 && split.length >= 1;
    }

    private boolean validChars(String s){
        return !s.startsWith("//") && !s.isEmpty();
    }

    private String trimString(String s){
        if(!s.startsWith("//") && s.contains("//")){
            return s.split("//")[0].trim();
        }
        return s;
    }

    private void boot() throws IOException {
        String s = String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                "@256","D=A","@SP","M=D","@300","D=A","@LCL","M=D","@400","D=A","@ARG","M=D","@3000","D=A","@THIS","M=D",
                "@4000","D=A","@THAT","M=D");
        s += callOperation("Sys.init",0);
        writeToOutputFile(tab(s));
    }

    private void stringOperation(String s) throws IOException {
        String[] ops = s.split(" ");
        if(ops.length == 2 && s.startsWith("label")){
            labelOperation(ops[1]);
        }
        else if(ops.length == 2){
            jumpOperation(ops.clone());
        }
        else if(ops.length == 3 && (ops[0].startsWith("function") || ops[0].startsWith("call"))){
            functionOperation(ops[1],ops[2],ops[0]);
        }
        else if(ops.length == 3){
            stackOperation(ops[0],ops[1],ops[2]);
        }
        else if(ops.length == 1 && ops[0].equals("return")){
            returnOperation();
        }
        else if(ops.length == 1){
            String s0 = arithmeticLogic(ops[0]);
            String s1 = compareLogic(ops[0]);
            String s2 = singleStackUsage(ops[0]);
            String finalS = null;
            if(s0 != null){
                finalS = s0;
            }
            else if(s1 != null){
                finalS = s1;
            }
            else if(s2 != null){
                finalS = s2;
            }
            if(finalS == null){
                return;
            }
            writeToOutputFile(finalS);
        }
    }

    // use @R15 for LCL-pointer
    // @R14 stores the retAddress
    // TODO: 18.05.22
    private void returnOperation() throws IOException {
        String s = String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n" +
                        "%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                "@LCL", "D=M", "@R15", "M=D", "@5", "A=D-A", "D=M", "@R14", "M=D",
                "@SP", "A=M-1", "D=M",
                "@ARG", "A=M", "M=D", "@ARG", "D=M+1", "@SP", "M=D",
                "@R15", "A=M-1", "D=M", "@THAT", "M=D", "@2", "D=A", "@R15", "A=M-D", "D=M", "@THIS", "M=D",
                "@3", "D=A", "@R15", "A=M-D", "D=M", "@ARG", "M=D", "@4", "D=A", "@R15", "A=M-D", "D=M", "@LCL", "M=D",
                "@R14","A=M", "0;JMP");
        writeToOutputFile(tab(s));
        if(!classNames.isEmpty()){
            classNames.pop();
        }
    }

    private void writeToOutputFile(String s) throws IOException {
        fileOutputStream.write(s.getBytes());
    }

    private String callOperation(String method, Integer i){
         String s = String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n" +
                        "%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                "@ret"+FUNCTION_RETURN,"D=A","@SP","A=M","M=D","@SP","M=M+1","@LCL","D=M","@SP",
                "A=M","M=D","@SP","M=M+1","@ARG","D=M","@SP","A=M","M=D","@SP","M=M+1",
                "@THIS","D=M","@SP","A=M","M=D","@SP","M=M+1","@THAT","D=M","@SP","A=M","M=D","@SP","M=M+1",
                "@5","D=A","@SP","D=M-D","@"+i,"D=D-A","@ARG","M=D",
                "@SP","D=M","@LCL","M=D","@"+method,"0;JMP","(ret"+FUNCTION_RETURN+")");
        FUNCTION_RETURN++;
        return s;
    }

    // if push/pop in function -> don't use @SP instead use local usw. in Stack
    private void functionOperation(String method , String param, String op) throws IOException {
        int i = Integer.parseInt(param);
        String build;
        if(op.equals("call")){
            build = callOperation(method,i);
        }
        else{
            classNames.push(method.split("[.]")[0]);
            String s = "("+ method +")\n";
            for(int k = 0; k < i; k++){
                s += String.format("%s\n%s\n%s\n%s\n%s\n","@SP","A=M","M=0","@SP","M=M+1");
            }
            build = s;
        }
        writeToOutputFile(tab(build));
    }

    private void jumpOperation(String[] s) throws IOException {
        String build;
        if(s[0].equals("goto")){
            build = String.format("%s\n%s\n","@"+s[1],"0;JMP");
        }
        else{
            build = String.format("%s\n%s\n%s\n%s\n%s\n%s\n","@SP","M=M-1","A=M","D=M","@"+s[1],"D;JNE");
        }
        writeToOutputFile(tab(build));
    }

    private void labelOperation(String label) throws IOException {
        String s = String.format("%s%s%s","(",label,")");
        writeToOutputFile(tab(s));
    }

    private void stackOperation(String stackCommand,String segment, String number) throws IOException {
        String finalString;
        if(stackCommand.equals("push")){
            if(segment.equals("constant")){
                finalString = pushConstant(number);
            }
            else{
                finalString = pushSegment(segment,number);
            }
        }
        //pop
        else{
            finalString = popSegment(number,segment);
        }
        writeToOutputFile(finalString);
    }

    public void translate(List<String> s) throws IOException {
        for(String c : s){
            c = trimString(c);
            if(validChars(c) && validLength(c)){
                stringOperation(c);
            }
        }
    }

    // push constant to stack
    private String pushConstant(String number){
        return tab(String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                "@"+number,"D=A","@SP","A=M","M=D","@SP","M=M+1"));
    }

    // push the element of the segment to the stack
    private String pushSegment(String segment, String number){
        String segmentNotation = mapSegments.get(segment);
        if(segmentNotation == null && segment.equals("temp")){
            return tab(buildTempPush(number));
        }
        else if(segmentNotation == null && segment.equals("static")){
            return tab(buildStaticPush(number));
        }
        else if(segmentNotation == null && segment.equals("pointer")){
            return tab(buildPointerPush(number));
        }
        return tab(String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                segmentNotation,"D=M","@"+number,"A=D+A","D=M","@SP","A=M","M=D","@SP","M=M+1"));
    }

    // pop the element of the stack and write it to the segment
    private String popSegment(String number, String segment){
        String segmentNotation = mapSegments.get(segment);
        if(segmentNotation == null && segment.equals("temp")){
            return tab(buildTempPop(number));
        }
        else if(segmentNotation == null && segment.equals("static")){
            return tab(buildStaticPop(number));
        }
        else if(segmentNotation == null && segment.equals("pointer")){
            return tab(buildPointerPop(number));
        }
        return tab(String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                "@SP","M=M-1",segmentNotation,"D=M","@"+number,"D=A+D","@R14","M=D","@SP","A=M","D=M","@R14","A=M","M=D"));
    }
    private String tab(String instruction){
        return Arrays.stream(instruction.split("[\n]")).map(x -> {
            if(x.startsWith("(")){
                return x+"\n";
            }
            return TAB_GENERATOR+x+"\n";
        }).reduce("", (z, y) -> z += y);
    }

    private String buildPointerPush(String number){
        // if number = 0 access THIS (address) else number = 1 THAT (address)
        String s = String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n","D="+number,
                "@VP"+POINTER_LABEL,"D;JEQ","@THAT","D=M","@ENDVP"+POINTER_LABEL,"0;JMP","(VP"+POINTER_LABEL+")",
                "@THIS","D=M","(ENDVP"+POINTER_LABEL+")","@SP","A=M","M=D","@SP","M=M+1");
        POINTER_LABEL++;

        return s;
    }

    // if pointer 0 write Address into THIS , if pointer 1 write Address into THAT
    private String buildPointerPop(String number){
        String s = String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                "D="+number,"@SP","M=M-1","@VP"+POINTER_LABEL,"D;JEQ","@SP","A=M","D=M","@THAT","M=D",
                "@ENDVP"+POINTER_LABEL,"0;JMP","(VP"+POINTER_LABEL+")","@SP","A=M","D=M","@THIS","M=D",
                "(ENDVP"+POINTER_LABEL+")");
        POINTER_LABEL++;
        return s;
    }

    // TODO: 21.05.22  Static-Logic umbauen -> Jede Klasse soll ihr eigenes Segment haben
    // Jede Klasse bekommt ihr eigenen Map-Eintrag -> falls static-op aufgerufen wird schaue in der Liste der Klasse
    // und hole den Eintrag der im i-ten Slot steht
    // BSP : @Foo.0 . STATIC_COUNTER bereits bei 3 -> holt sich den 3-Eintrag aus der List für die Klasse und
    // das ist dann der Index im Static Segment

    private String buildStaticPush(String number){
        String call = "@Foo."+number;
        int i = Integer.parseInt(number);
        if(!classNames.isEmpty()){
            call = "@Foo." + staticMap.get(classNames.peek()).get(i);
        }
        return String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                call,"D=M","@SP","A=M","M=D","@SP","M=M+1");
    }

    private String buildStaticPop(String number){
        String call = "@Foo."+number;
        int i = Integer.parseInt(number);
        if(!classNames.isEmpty()){
            staticMap.computeIfPresent(classNames.peek(),(x, y) -> {
                y.add(STATIC_COUNTER);
                return y;
            });
            call = "@Foo."+staticMap.get(classNames.peek()).get(i);
        }
        STATIC_COUNTER++;
        return String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                "@SP","M=M-1","@SP","A=M","D=M",call,"M=D");
    }

    private String buildTempPush(String number){
        int n = Integer.parseInt(number) + 5;
        return String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n","@"+ n,"D=M","@SP","A=M","M=D","@SP","M=M+1");
    }

    private String buildTempPop(String number){
        int n = Integer.parseInt(number) + 5;
        return String.format("%s\n%s\n%s\n%s\n%s\n%s\n","@SP","M=M-1","A=M","D=M","@"+n,"M=D");
    }

    private String arithmeticLogic(String operation){
        // not operation only uses SP-1
        String s = String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                "@SP", "M=M-1", "A=M", "D=M", "@SP", "M=M-1", "A=M");
        if(operation.equals("add")){
            s += "D=M+D\n";
        }
        else if(operation.equals("and")){
            s += "D=M&D\n";
        }
        else if(operation.equals("or")){
            s += "D=M|D\n";
        }
        else if(operation.equals("sub")){
            s += "D=M-D\n";
        }
        else{
            return null;
        }
        s += String.format("%s\n%s\n%s\n%s\n%s\n","@SP","A=M","M=D","@SP","M=M+1");
        return tab(s);
    }

    private String compareLogic(String string){
        String s = String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                "@SP","M=M-1","A=M","D=M","@SP","M=M-1","A=M","D=M-D","@V1"+COMPARE_LABEL);
        if(string.equals("eq")){
            s += String.format("%s\n","D;JEQ");
        }
        else if(string.equals("lt")){
            s += String.format("%s\n","D;JLT");
        }
        else if(string.equals("gt")){
            s += String.format("%s\n","D;JGT");
        }
        else{
            return null;
        }
        s += String.format("%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n",
                "@SP","A=M","M=0","@END"+COMPARE_LABEL,"0;JMP","(V1"+COMPARE_LABEL+")",
                "@SP","A=M","M=-1","(END"+COMPARE_LABEL+")","@SP","M=M+1");
        COMPARE_LABEL++;
        return tab(s);
    }

    // not and neg
    private String singleStackUsage(String operation){
        String s = String.format("%s\n%s\n%s\n%s\n",
                "@SP","M=M-1","A=M","D=M");
        if(operation.equals("not")){
            s += String.format("%s\n","D=!D");
        }
        else if(operation.equals("neg")){
            s += String.format("%s\n","D=-D");
        }
        else{
            return null;
        }
        s += String.format("%s\n%s\n%s\n","M=D","@SP","M=M+1");
        return tab(s);
    }

    // wenn das Directory eine Main hat -> dann fange damit an Sys.init aufzurufen

    private List<String> fileOperation(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        Scanner scanner;
        if(file.isDirectory()){
            File[] files = file.listFiles();
            if(files == null){
                return null;
            }
            boot();
            for(File f : files){
                if(!f.getName().contains(".vm")){
                    continue;
                }
                staticMap.putIfAbsent(f.getName().split("[.]")[0],new ArrayList<>());
                scanner = new Scanner(f);
                while (scanner.hasNextLine()){
                    lines.add(scanner.nextLine());
                }
                scanner.close();
            }
        }
        else{
            staticMap.put(file.getName().split("[.]")[0],new ArrayList<>());
            scanner = new Scanner(file);
            while (scanner.hasNextLine()){
                lines.add(scanner.nextLine());
            }
            scanner.close();
        }
        return lines;
    }

    // TODO: 20.05.22 check between File and Directory
    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        File fileOutput = new File(args[1]);

        Translator translator = new Translator(fileOutput);
        List<String> content = translator.fileOperation(file);

        translator.translate(Objects.requireNonNull(content));
    }
}
