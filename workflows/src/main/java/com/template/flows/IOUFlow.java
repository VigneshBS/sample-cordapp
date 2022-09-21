package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.IOUContract;
import com.template.states.IOUState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.util.Arrays;

public class IOUFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<Void>{

        private int iouValue;
        private Party otherParty;

        private final ProgressTracker progressTracker = new ProgressTracker();

        public Initiator(int iouValue, Party otherParty) {
            this.iouValue = iouValue;
            this.otherParty = otherParty;
        }

        public int getIouValue() {
            return iouValue;
        }

        public Party getOtherParty() {
            return otherParty;
        }

        @Nullable
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {

            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            final IOUState output = new IOUState(iouValue,getOurIdentity(),otherParty);

            Command command = new Command(new IOUContract.Create(), Arrays.asList(getOurIdentity().getOwningKey(),otherParty.getOwningKey()));

            final TransactionBuilder builder = new TransactionBuilder(notary);
            builder.addOutputState(output);
            builder.addCommand(command);
            builder.verify(getServiceHub());

            final SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(builder);

            FlowSession otherPartySession = initiateFlow(otherParty);

            SignedTransaction fullySigned = subFlow(new CollectSignaturesFlow(signedTransaction,Arrays.asList(otherPartySession),CollectSignaturesFlow.tracker()));

            subFlow(new FinalityFlow(fullySigned,otherPartySession));

            return null;
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void>{
        private FlowSession otherPartySession;

        public Responder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {

            class SignedTxFlow extends SignTransactionFlow{

                public SignedTxFlow(@NotNull FlowSession otherSideSession) {
                    super(otherSideSession);
                }

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("Transaction must be of IOUState",output instanceof IOUState);
                        IOUState iou = (IOUState) output;
                        require.using("Minimum amount is 100",iou.getValue()>=100);
                        return null;
                    });
                }
            }

            SecureHash expectedId = subFlow(new SignedTxFlow(otherPartySession)).getId();

            subFlow(new ReceiveFinalityFlow(otherPartySession,expectedId));
            return null;
        }
    }

}