package com.template.contracts;

import com.template.states.IOUState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


public class IOUContract implements Contract {
    public static final String ID = "com.template.contracts.TemplateContract";

    @Override
    public void verify(LedgerTransaction tx) {
        CommandWithParties<IOUContract.Create> command = requireSingleCommand(tx.getCommands(),IOUContract.Create.class);

        if(!tx.getInputs().isEmpty()){
            throw new IllegalArgumentException("No input should be consumed");
        }
        if(!(tx.getOutputs().size()==1)){
            throw new IllegalArgumentException("Only one input");
        }

        final IOUState output = tx.outputsOfType(IOUState.class).get(0);
        final Party lender = output.getLender();
        final Party borrower = output.getBorrower();

        if(output.getValue()<=0){
            throw new IllegalArgumentException("IOU must be positive");
        }
        if(lender.equals(borrower)){
            throw new IllegalArgumentException("Cannot be same");
        }

    }


    public static class Create implements CommandData {
    }
}