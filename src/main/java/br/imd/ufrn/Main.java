package br.imd.ufrn;

import br.imd.ufrn.gateway.GatewayMain;
import br.imd.ufrn.instance.InstanceMain;

/**
 * <pre>
 *   java ... br.imd.ufrn.Main gateway
 *   java ... br.imd.ufrn.Main br br-1 9101
 *   java ... br.imd.ufrn.Main pt pt-1 9201 [gatewayHost] [advertiseHost]
 * </pre>
 */
public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            usage();
            return;
        }

        String cmd = args[0].trim().toLowerCase();
        switch (cmd) {
            case "gateway", "gw" -> GatewayMain.main(new String[0]);
            case "br", "pt" -> InstanceMain.main(args);
            default -> {
                System.err.println("comando desconhecido: " + args[0]);
                usage();
            }
        }
    }

    private static void usage() {
        System.out.println("""
                Uso:
                  Main gateway
                  Main <br|pt> <instanceId> <port> [gatewayHost] [advertiseHost]

                Exemplos:
                  Main gateway
                  Main br br-1 9101
                  Main br br-2 9102
                  Main pt pt-1 9201
                """);
    }
}
