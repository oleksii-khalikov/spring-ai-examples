package org.springframework.ai.mcp.samples.sqlite;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.client.McpClient;
import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.mcp.client.stdio.ServerParameters;
import org.springframework.ai.mcp.client.stdio.StdioServerTransport;
import org.springframework.ai.mcp.spring.McpFunctionCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    @Bean
    @Profile("!chat")
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
                                                 List<McpFunctionCallback> functionCallbacks,
                                                 ConfigurableApplicationContext context) {
        return args -> {
            var chatClient = chatClientBuilder.defaultFunctions(functionCallbacks.toArray(new McpFunctionCallback[0]))
                    .build();
            runPredefinedQuestions(chatClient, context);
        };
    }

    @Bean
    @Profile("chat")
    public CommandLineRunner interactiveChat(ChatClient.Builder chatClientBuilder,
                                             List<McpFunctionCallback> functionCallbacks,
                                             ConfigurableApplicationContext context) {
        return args -> {
            var chatClient = chatClientBuilder.defaultFunctions(functionCallbacks.toArray(new McpFunctionCallback[0]))
                    .build();
            startInteractiveChat(chatClient, context);
        };
    }

    private void startInteractiveChat(ChatClient chatClient, ConfigurableApplicationContext context) {
        var scanner = new Scanner(System.in);
        System.out.println("\nStarting interactive chat session. Type 'exit' to quit.");

        try {
            while (true) {
                System.out.print("\nUSER: ");
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("Ending chat session.");
                    break;
                }

                System.out.print("ASSISTANT: ");
                System.out.println(chatClient.prompt(input).call().content());
            }
        } finally {
            scanner.close();
            context.close();
        }
    }

    private void runPredefinedQuestions(ChatClient chatClient, ConfigurableApplicationContext context) {
        System.out.println("Running predefined questions with AI model responses:\n");

        // Question 1
        String question1 = "Can you connect to my SQLite database and tell me what products are available, and their prices?";
        System.out.println("QUESTION: " + question1);
        System.out.println("ASSISTANT: " + chatClient.prompt(question1).call().content());

        // Question 2
        String question2 = "What's the average price of all products in the database?";
        System.out.println("\nQUESTION: " + question2);
        System.out.println("ASSISTANT: " + chatClient.prompt(question2).call().content());

        // Question 3
        String question3 = "Can you analyze the price distribution and suggest any pricing optimizations?";
        System.out.println("\nQUESTION: " + question3);
        System.out.println("ASSISTANT: " + chatClient.prompt(question3).call().content());

        // Question 4
        String question4 = "Could you help me design and create a new table for storing customer orders?";
        System.out.println("\nQUESTION: " + question4);
        System.out.println("ASSISTANT: " + chatClient.prompt(question4).call().content());

        System.out.println("\nPredefined questions completed. Exiting application.");
        context.close();
    }

    @Bean
    public McpFunctionCallback[] functionCallbacks(McpSyncClient mcpClient) {

        return mcpClient.listTools(null)
                .tools()
                .stream()
                .map(tool -> new McpFunctionCallback(mcpClient, tool))
                .toArray(McpFunctionCallback[]::new);
    }

    @Bean(destroyMethod = "close")
    public McpSyncClient mcpClient() {

        var stdioParams = ServerParameters.builder("uvx")
                .args("mcp-server-sqlite", "--db-path",
                        getDbPath())
                .build();

        var mcpClient = McpClient.sync(new StdioServerTransport(stdioParams),
                Duration.ofSeconds(10), new ObjectMapper());

        var init = mcpClient.initialize();

        System.out.println("MCP Initialized: " + init);

        return mcpClient;

    }


    private static String getDbPath() {
        return Paths.get(System.getProperty("user.dir"), "test.db").toString();
    }

}