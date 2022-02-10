package benchmark.rdfsreasoning;

import org.rdfhdt.hdt.triples.TripleID;
import reasoning.saturation.models.WorkerModel;
import reasoning.saturation.workload.WorkloadDistributor;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class RDFSWorkloadDistributor extends WorkloadDistributor<RDFSClosure, TripleID> {

    private long domainID;
    private long rangeID;
    private long rdfTypeID;
    private long subPropertyOfID;
    private long subClassOfID;
    private long rdfPropertyID;
    private long rdfsClass;


    protected RDFSWorkloadDistributor() {

    }



    @Override
    public Stream<Long> getRelevantWorkerIDsForAxiom(TripleID axiom) {
        /*
        Stream.Builder<Long> relevantWorkers = Stream.builder();

        for (WorkerModel<RDFSClosure, TripleID> worker : this.workerModels) {
            long predicateID = axiom.getPredicate();
            Set<Long> workerPropertyIDs = worker.getWorkerTerms().getPropertyIDs();

            // properties
            if (workerPropertyIDs.contains(predicateID)) {
                relevantWorkers.add(worker.getID());
                continue;
            } else if ((predicateID == domainID || predicateID == rangeID)
                        && workerPropertyIDs.contains(axiom.getSubject())) {
                relevantWorkers.add(worker.getID());
                continue;
            } else if (predicateID == subPropertyOfID &&
                    (workerPropertyIDs.contains(axiom.getSubject()) || workerPropertyIDs.contains(axiom.getObject()))) {
                relevantWorkers.add(worker.getID());
                continue;
            } else if (predicateID == rdfTypeID &&
                    axiom.getObject() == rdfPropertyID &&
                    workerPropertyIDs.contains(axiom.getSubject())) {
                relevantWorkers.add(worker.getID());
                continue;
            }

            // RDF class
            Set<Long> workerRDFClassIDs = worker.getWorkerTerms().getPropertyIDs();
            long sbjID = axiom.getSubject();
            long objID = axiom.getObject();
            if (predicateID == rdfTypeID && objID == rdfsClass && workerRDFClassIDs.contains(sbjID)) {
                relevantWorkers.add(worker.getID());
            } else if (predicateID == rdfTypeID && workerRDFClassIDs.contains(objID)) {
                relevantWorkers.add(worker.getID());
            } else if (predicateID == subClassOfID
                    && (workerRDFClassIDs.contains(sbjID) || workerRDFClassIDs.contains(objID))) {
                relevantWorkers.add(worker.getID());
            }

            // TODO: consider RDFS13 and RDFS12

        }
         */

        return null;
    }
}
