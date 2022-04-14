package ca.utoronto.utm.mcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.neo4j.driver.*;

public class Utils {
   public static String uriDb = "bolt://neo4j:7687";
   public static Driver driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "123456"));

   public static String convert(InputStream inputStream) throws IOException {

      try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
         return br.lines().collect(Collectors.joining(System.lineSeparator()));
      }
   }
}
