import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class Assembler {
    File fileOutput;
    FileOutputStream fileOutputStream;
    Map<String,Integer> mapping = new HashMap<>();
    Map<String,String> compA_0 = new HashMap<>();
    Map<String,String> compA_1 = new HashMap<>();
    Map<String,String> dest = new HashMap<>();
    Map<String,String> jumps = new HashMap<>();
    int globalCounter = 16;

    private void writeTo(String s) throws IOException {
        String allowedStrings = extractString(s);
        compute_IfAbsent(allowedStrings);
        doOperation(allowedStrings);
    }

    private void fillMap() {
        mapping.put("R0",0);
        mapping.put("R1",1);
        mapping.put("R2",2);
        mapping.put("R3",3);
        mapping.put("R4",4);
        mapping.put("R5",5);
        mapping.put("R6",6);
        mapping.put("R7",7);
        mapping.put("R8",8);
        mapping.put("R9",9);
        mapping.put("R10",10);
        mapping.put("R11",11);
        mapping.put("R12",12);
        mapping.put("R13",13);
        mapping.put("R14",14);
        mapping.put("R15",15);
        mapping.put("SCREEN",16384);
        mapping.put("KBD",24576);
        mapping.put("SP",0);
        mapping.put("LCL",1);
        mapping.put("ARG",2);
        mapping.put("THIS",3);
        mapping.put("THAT",4);

        compA_0.put("0","101010");
        compA_0.put("1","111111");
        compA_0.put("-1","111010");
        compA_0.put("D","001100");
        compA_0.put("A","110000");
        compA_0.put("!D","001101");
        compA_0.put("!A","110001");
        compA_0.put("-D","001111");
        compA_0.put("-A","110011");
        compA_0.put("D+1","011111");
        compA_0.put("A+1","110111");
        compA_0.put("D-1","001110");
        compA_0.put("A-1","110010");
        compA_0.put("D+A","000010");
        compA_0.put("D-A","010011");
        compA_0.put("A-D","000111");
        compA_0.put("D&A","000000");
        compA_0.put("D|A","010101");

        compA_1.put("M","110000");
        compA_1.put("!M","110001");
        compA_1.put("-M","110011");
        compA_1.put("M+1","110111");
        compA_1.put("M-1","110010");
        compA_1.put("D+M","000010");
        compA_1.put("D-M","010011");
        compA_1.put("M-D","000111");
        compA_1.put("D&M","000000");
        compA_1.put("D|M","010101");

        dest.put("M","001");
        dest.put("D","010");
        dest.put("MD","011");
        dest.put("A","100");
        dest.put("AM","101");
        dest.put("AD","110");
        dest.put("AMD","111");

        jumps.put("JGT","001");
        jumps.put("JEQ","010");
        jumps.put("JGE","011");
        jumps.put("JLT","100");
        jumps.put("JNE","101");
        jumps.put("JLE","110");
        jumps.put("JMP","111");
    }

    private void controlLines(List<String> lines) {
        int lineCounter = 0;
        for(String s : lines){
            if(s.startsWith("(")){
                mapping.put(s.substring(1,s.length()-1),lineCounter);
            }
            if(!s.startsWith("//") && !s.isEmpty() && !s.startsWith("(")){
                lineCounter++;
            }
        }
    }

    private void doOperation(String a) throws IOException {
        String s;
        if(a != null){
            if(a.startsWith("@")) {
                Integer i = null;
                try{
                    i = Integer.parseInt(a.substring(1));
                }
                catch (Exception ignored){}
                Integer valueStored;
                if(i == null){
                    valueStored = mapping.get(a.substring(1));
                }
                else{
                    valueStored = i;
                }
                s = binaryValue(valueStored);
            }
            else{
                s = doStringOp(a);
                if(s == null){
                    return;
                }
            }
            fileOutputStream.write(s.getBytes());
        }
    }

    private String doStringOp(String a) {
        String finalString = "111";
        String[] split = a.split("=");
        String [] splitSemicolon = a.split(";");
        String destination;
        String comp;
        String jump;
        if(a.contains("(")){
            return null;
        }
        // 0;JMP
        if(split.length == 1){
            comp = splitSemicolon[0];
            jump = splitSemicolon[1];
            finalString = checkIf_AorM(finalString,comp);
            finalString += "000" + jumps.get(jump);
        }
        // D=A
        else{
            destination = split[0];
            comp = split[1];
            finalString = checkIf_AorM(finalString,comp);
            finalString += dest.get(destination) + "000";
        }
        return finalString + "\n";
    }

    private void compute_IfAbsent(String a){
        if(a == null){
            return;
        }
        if(a.contains("@")){
            try{
                int i = Integer.parseInt(a.substring(1));
            }
            catch (Exception e){
                mapping.computeIfAbsent(a.substring(1), x -> globalCounter++);
            }
        }
    }

    private String checkIf_AorM(String finalString, String comp){
        if(comp.contains("M")){
            finalString += "1";
            finalString += compA_1.get(comp);
        }
        else{
            finalString += "0";
            finalString += compA_0.get(comp);
        }
        return finalString;
    }

    private String extractString(String s){
        String trimmedString = s.trim();
        if(trimmedString.isEmpty() || trimmedString.startsWith("/")){
            return null;
        }
        if(trimmedString.contains("/")){
            trimmedString = trimmedString.split("//")[0].trim();
        }
        return trimmedString;
    }

    private String binaryValue(Integer value){
        String s = "";
        int mod;
        while (value != 0){
            mod = value % 2;
            value = value / 2;
            s += mod;
        }
        int var = 16 - s.length();
        while (var != 0){
            s += "0";
            var--;
        }
        String reverse = "";
        for(int i = 15; i >= 0; i--){
            reverse += s.charAt(i);
        }
        reverse+="\n";
        return reverse;
    }

    private void file_Operations() throws IOException {
        fileOutput = new File("Assembler.hack");
        if(fileOutput.length() != 0){
            fileOutput.delete();
            fileOutput.createNewFile();
        }
    }

    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        Assembler assembler = new Assembler();
        assembler.file_Operations();
        assembler.fileOutputStream = new FileOutputStream(assembler.fileOutput,true);
        assembler.fillMap();
        List<String> lines = new ArrayList<>();
        try(Scanner scanner = new Scanner(file)){
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
            assembler.controlLines(lines);
            lines.forEach(x -> {
                try {
                    assembler.writeTo(x);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
