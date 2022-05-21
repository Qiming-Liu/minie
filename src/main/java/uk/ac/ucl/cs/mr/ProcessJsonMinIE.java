package uk.ac.ucl.cs.mr;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.csvreader.CsvWriter;
import de.uni_mannheim.minie.MinIE;
import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.annotation.AnnotatedProposition;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class ProcessJsonMinIE {


    public static int total = 0;
    public static int[] done;
    public static long startTime = 0;

    public static void process(String path, int nThreads) {
        String json = readJsonFile(path);
        JSONArray jsonArray = JSON.parseArray(json);
        List<Fact> facts = new ArrayList<>();

        total = jsonArray.size();
        done = new int[nThreads];

        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        CountDownLatch latch = new CountDownLatch(nThreads);

        startTime = System.currentTimeMillis();

        for (int i = 0; i < nThreads; i++) {
            final List<Object> task = jsonArray.subList(total / nThreads * i, total / nThreads * (i + 1));
            int fi = i;

            executorService.execute(() -> {
                try {
                    facts.addAll(oneTask(task, fi));
                } finally {
                    latch.countDown();
                    System.out.println(latch.getCount());
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException ignored) {

        }

        // need to create csv file first
        writeCsvFile(path + ".csv", facts);
        executorService.shutdown();
    }

    public static List<Fact> oneTask(List<Object> jsonArray, int index) {
        List<Fact> facts = new ArrayList<>();


        for (int i = 0; i < jsonArray.size(); i++) {
            if (((String) jsonArray.get(i)).length() > 1) { //sometimes null string
                FactsBean factsBean = query((String) jsonArray.get(i));
                int fi = i;
                factsBean.facts.forEach(fact -> fact.sentence = (String) jsonArray.get(fi));
                facts.addAll(factsBean.facts);
            }

            if (i % 200 == 0 & i != 0) {
                done[index] = i;

                long currTime = System.currentTimeMillis() - startTime;
                long doneSum = IntStream.of(done).sum();
                long avgTime = currTime / doneSum;
                long leftTime = avgTime * (total - doneSum);

                //print like tqdm
                String print = doneSum + "/" + total + " avgTime(s):" + String.format("%-8s", avgTime / 1000.0) + " currTime(s):" + String.format("%-8s", currTime / 1000.0) + " leftTime(s):" + String.format("%-8s", leftTime / 1000.0);
                System.out.println("Thread " + index + ": " + print);
            }
        }

        return facts;
    }

    public static String readJsonFile(String fileName) {
        String jsonStr = "";
        try {
            File jsonFile = new File(fileName);
            FileReader fileReader = new FileReader(jsonFile);

            Reader reader = new InputStreamReader(new FileInputStream(jsonFile), "utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static FactsBean query(String sentence) {
        MinIE minie = new MinIE(sentence, CoreNLPUtils.StanfordDepNNParser(), MinIE.Mode.SAFE);

        List<Fact> facts = new ArrayList<>();

        for (AnnotatedProposition ap : minie.getPropositions()) {
            List<AnnotatedPhrase> triple = ap.getTriple();

            String s = "";
            String p = "";
            String o = "";
            try {
                s = triple.get(0).toString();
                p = triple.get(1).toString();
                o = triple.get(2).toString();
            } catch (IndexOutOfBoundsException ignored) {
            }

            Fact fact = new Fact(s, p, o);
            facts.add(fact);
        }

        return new FactsBean(facts);
    }

    public static void writeCsvFile(String writeCsvFilePath, List<Fact> facts) {
        CsvWriter csvWriter = new CsvWriter(writeCsvFilePath, ',', StandardCharsets.UTF_8);

        try {
            String[] headers = {"subject", "relation", "object", "sentence"};
            csvWriter.writeRecord(headers);

            for (int i = 0; i < facts.size(); i++) {
                String subject = facts.get(i).subject;
                String predicate = facts.get(i).predicate;
                String object = facts.get(i).object;
                String sentence = facts.get(i).sentence;
                String[] output = new String[]{subject, predicate, object, sentence};

                csvWriter.writeRecord(output);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            csvWriter.close();
        }
    }
}
