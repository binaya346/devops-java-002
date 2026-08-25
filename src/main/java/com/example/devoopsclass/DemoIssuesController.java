package com.example.devoopsclass;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoIssuesController {

    // 🔴 1. SECURITY VULNERABILITY: Hardcoded Credentials / Secret
    private static final String ADMIN_PASSWORD = "SuperSecretAdminPassword123!";
    private static final String DB_API_KEY = "AIzaSyD-EXAMPLE_HARDCODED_KEY";

    // 🟡 2. CODE SMELL: Unused private field
    private int unusedCounter = 0;

    @GetMapping("/demo/issues")
    public String triggerIssues(@RequestParam(required = false) String input) {

        // 🔴 3. BUG: Comparing Strings using '==' instead of '.equals()'
        String defaultRole = new String("ADMIN");
        if (input == defaultRole) {
            System.out.println("User is admin");
        }
        

        // 🔴 4. BUG: Guaranteed NullPointerException (NPE)
        String nullableValue = null;
        if (input != null && input.equals("trigger-npe")) {
            nullableValue = null;
        }
        // SonarQube flags potential or definite dereference
        int len = nullableValue != null ? nullableValue.length() : 0;

        // 🔴 5. BUG: Resource Leak (Unclosed resource without try-with-resources)
        simulateResourceLeak();

        // 🔴 6. SECURITY HOTSPOT: Weak Hashing Algorithm (MD5)
        generateWeakHash("sample-password");

        // 🔴 7. SECURITY HOTSPOT: Insecure pseudo-random number generator
        int randomPin = generateInsecureRandomNumber();

        // 🟡 8. CODE SMELL: Empty catch block (Swallowing exception)
        try {
            int divideByZero = 100 / (input != null ? Integer.parseInt(input) : 1);
        } catch (Exception e) {
            // Empty catch block - SonarQube flags this as a Code Smell
        }

        // 🟡 9. CODE SMELL: Dead / Unreachable code
        if (false) {
            System.out.println("This code will never execute!");
        }

        return "Demo issues evaluated! Random PIN: " + randomPin;
    }

    // Bug: Missing proper close / try-with-resources
    private void simulateResourceLeak() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("dummy.txt"));
            String line = reader.readLine();
            System.out.println(line);
            // Missing reader.close() -> Resource Leak
        } catch (IOException e) {
            // Ignored error
        }
    }

    // Security: MD5 is cryptographically broken
    private String generateWeakHash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes());
            return new String(hash);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    // Security: java.util.Random is not cryptographically secure
    private int generateInsecureRandomNumber() {
        Random random = new Random();
        return random.nextInt(10000);
    }

    // 🟡 10. CODE SMELL: Unused private method
    private void deadMethodThatIsNeverCalled() {
        System.out.println("Nobody calls me");
    }
}
