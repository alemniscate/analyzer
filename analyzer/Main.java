package analyzer;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        File file1 = new File(args[0]);
        String fileName1 = file1.getAbsolutePath();
        System.out.println(fileName1 + " exists " + file1.exists());
        System.out.println(fileName1 + " isDirectory " + file1.isDirectory());

        File file2 = new File(args[1]);
        String fileName2 = file2.getAbsolutePath();
        System.out.println(fileName2 + " exists " + file2.exists());
        System.out.println(fileName2 + " isDirectory " + file2.isDirectory());

        try {
            ExecutorService executor = Executors.newFixedThreadPool(10);

            App app = new App(args);
            String[] list = app.getFiles();
            String[] db = app.getDb();

            List<Callable<String>> callables = new ArrayList<>();
            for (String fileName: list) { 
                callables.add(() -> {
                    Search search = new Search(fileName, db);
                    return search.execute();
                });
            }

            List<Future<String>> futures = executor.invokeAll(callables);

            for (Future<String> future: futures) {
                System.out.println(future.get());
            }

            executor.shutdown();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println(e.getMessage());
        }
    }
}

class App {
    String dbName;
    String folderName;

    App(String[] args) {
        dbName = args[1];
        folderName = args[0];
    }

    String[] getFiles() {
        File dir = new File(folderName);
        File[] fileList = dir.listFiles();
        String[] list = new String[fileList.length]; 
        for (int i = 0; i < list.length; i++) {
            list[i] = fileList[i].getAbsolutePath();
        }
        return list;
    }

    String[] getDb() {
        TextRead tr = new TextRead();
        String[] db = tr.readAll(dbName);
        quateCut(db);
        tr.sortDescending(db);
        return db;
    }

    void quateCut(String[] db) {
        for (int i = 0; i < db.length; i++) {
            db[i] = db[i].replace("\"", "");
        }
    }
}

class Search {
    String fileName;
    String[] db;

    ByteRead br;
    byte[] bytes;
    String fileType;

    Search(String fileName, String[] db) {
        this.fileName = fileName;
        this.db = db;

        br = new ByteRead();
        bytes = br.readAll(fileName);
    }

    String execute() {
        kmp();
        File file = new File(fileName);
        return file.getName() + ": " + fileType;
    }

    void kmp() {
        fileType = "";
        for (String rec: db) {
            String[] strs = rec.split(";");
            String pattern = strs[1];
            String type = strs[2];
            List<Integer> list = KMPSearch(bytes, pattern);
            if (!list.isEmpty()) {
                fileType = type;
                return;
            }
        }
        fileType = "Unknown file type";
    }

    List<Integer> KMPSearch(byte[] bytes, String pattern) {
        
        int[] prefixFunc = prefixFunction(pattern);
        ArrayList<Integer> occurrences = new ArrayList<Integer>();
        int j = 0;
        for (int i = 0; i < bytes.length; i++) {
            while (j > 0 && bytes[i] != pattern.charAt(j)) {
                j = prefixFunc[j - 1];
            }
            if (bytes[i] == pattern.charAt(j)) {
                j += 1;
            }
            if (j == pattern.length()) {
                occurrences.add(i - j + 1);
                j = prefixFunc[j - 1];
            }
        }
        return occurrences;
    }
    
    int[] prefixFunction(String str) {
    
        int[] prefixFunc = new int[str.length()];
        for (int i = 1; i < str.length(); i++) {
            int j = prefixFunc[i - 1];
            while (j > 0 && str.charAt(i) != str.charAt(j)) {
                j = prefixFunc[j - 1];
            }
            if (str.charAt(i) == str.charAt(j)) {
                j += 1;
            }
            prefixFunc[i] = j;
        }
        return prefixFunc;
    }
}

class ByteRead{

    long fileSize;

    byte[] readAll(String fileName) {
        try (InputStream sr = new FileInputStream(fileName);) {
            fileSize = new File(fileName).length();
            byte[] bytes= new byte[(int) fileSize];
            sr.read(bytes);
            sr.close();
            return bytes;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return new byte[0];
        }
    }
}

class TextRead{

    String[] readAll(String fileName) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            List<String> list = new ArrayList<>();
            String rec = "";
            while ((rec = br.readLine()) != null) {
                list.add(rec);
            }
            String[] array = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                array[i] = list.get(i);
            }
            br.close();
            return array;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return new String[0];
        }
    }

    void sortDescending(String[] array) {

        for (int i = 1; i < array.length; i++) {        
            String elem = array[i];
            int j = i - 1;
             
            while (j >= 0 && array[j].compareTo(elem) < 0) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = elem;
        }
    }
}

class TimeDuration {
    long time;

    TimeDuration(long startTime, long endTime) {
        time = endTime - startTime;
    }

    double getSeconds() {
        long mm = time / 1_000_000L;
        return (double) mm / 1000.0;
    }
}
