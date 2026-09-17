package br.imd.ufrn;

import br.imd.ufrn.component.ReservationsComponent;
import br.imd.ufrn.component.TablesComponent;
import br.imd.ufrn.gateway.ApiGateway;
import br.imd.ufrn.models.ServerRole;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "gateway" -> new ApiGateway().start();
            case "tables" -> startTables(args);
            case "reservations" -> startReservations(args);
            default -> printUsage();
        }
    }

    private static void startTables(String[] args) throws Exception {
        int port = argInt(args, 1, 9101);
        String gatewayHost = arg(args, 2, "127.0.0.1");
        int controlPort = argInt(args, 3, ApiGateway.CONTROL_PORT);
        String advertiseHost = arg(args, 4, "127.0.0.1");
        new TablesComponent(port, gatewayHost, controlPort, advertiseHost).start();
    }

    private static void startReservations(String[] args) throws Exception {
        int port = argInt(args, 1, 9201);
        String gatewayHost = arg(args, 2, "127.0.0.1");
        int controlPort = argInt(args, 3, ApiGateway.CONTROL_PORT);
        String advertiseHost = arg(args, 4, "127.0.0.1");
        ServerRole role = ServerRole.valueOf(arg(args, 5, "LEADING").toUpperCase());
        new ReservationsComponent(port, gatewayHost, controlPort, advertiseHost, role).start();
    }

    private static String arg(String[] args, int index, String defaultValue) {
        return args.length > index ? args[index] : defaultValue;
    }

    private static int argInt(String[] args, int index, int defaultValue) {
        return args.length > index ? Integer.parseInt(args[index]) : defaultValue;
    }

    private static void printUsage() {
        System.out.println("""
                Uso:
                  java -cp target/classes br.imd.ufrn.Main gateway
                  java -cp target/classes br.imd.ufrn.Main tables [port] [gatewayHost] [controlPort] [advertiseHost]
                  java -cp target/classes br.imd.ufrn.Main reservations [port] [gatewayHost] [controlPort] [advertiseHost] [LEADING|FOLLOWING]

                Exemplos locais:
                  java -cp target/classes br.imd.ufrn.Main gateway
                  java -cp target/classes br.imd.ufrn.Main tables 9101
                  java -cp target/classes br.imd.ufrn.Main tables 9102
                  java -cp target/classes br.imd.ufrn.Main reservations 9201

                Teste HTTP:
                  curl http://127.0.0.1:8080/registry
                  curl http://127.0.0.1:8080/tables

                Teste TCP (porta 9091), uma linha:
                  ROUTE tables LIST
                """);
    }
}
