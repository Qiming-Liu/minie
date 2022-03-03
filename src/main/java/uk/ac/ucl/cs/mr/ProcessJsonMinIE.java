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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessJsonMinIE {
    //run this function to start process minie
    public static void process(String filename) {
        String json = readJsonFile("C:\\Users\\pross\\Desktop\\project\\github\\ie_script\\data_json\\" + filename + ".json");
        JSONArray jsonArray = JSON.parseArray(json);
        List<Fact> facts = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < jsonArray.size(); i++) {
            if (((String) jsonArray.get(i)).length() > 1) { //sometimes null string
                FactsBean factsBean = query((String) jsonArray.get(i));
                facts.addAll(factsBean.facts);
                long costTime = System.currentTimeMillis() - startTime;
                long avgTime = costTime / (i + 1);
                long leftTime = avgTime * (jsonArray.size() - i);

                //print like tqdm
                String print = (i + 1) + "/" + jsonArray.size() + " costTime(s):" + String.format("%-8s", costTime / 1000) + " leftTime(s):" + String.format("%-8s", leftTime / 1000) + " avgTime(s):" + String.format("%-8s", avgTime / 1000.0);
                System.out.println("[" + filename + "]" + print);
            }
        }

        // need to create csv file first
        writeCsvFile("C:\\Users\\pross\\Desktop\\project\\github\\ie_script\\result_minie\\" + filename + "r.csv", facts);
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
            String[] headers = {"subject", "relation", "object"};
            csvWriter.writeRecord(headers);

            for (int i = 0; i < facts.size(); i++) {
                csvWriter.writeRecord((String[]) Arrays.asList(facts.get(i).subject, facts.get(i).predicate, facts.get(i).object).toArray());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            csvWriter.close();
        }
    }
}
