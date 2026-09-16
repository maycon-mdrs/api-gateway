package br.imd.ufrn.exceptions;

public class LeaderOnlyOperationException extends CamaroesBaseException {

    public LeaderOnlyOperationException() {
    }

    @Override
    public String getFriendlyMessage() {
        return "Esta operação só pode ser realizada pelo servidor líder.";
    }

    @Override
    public String getLogMessage() {
        return "LeaderOnlyOperationException: servidor não líder tentou executar uma escrita.";
    }
}
