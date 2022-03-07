package util.serialization;

import benchmark.echoclosure.*;
import benchmark.microbenchmark.TestObject;
import benchmark.rdfsreasoning.RDFSClosure;
import benchmark.rdfsreasoning.rules.*;
import benchmark.transitiveclosure.*;
import enums.MessageDistributionType;
import networking.ServerData;
import networking.messages.*;
import nio2kryo.Edge;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.triples.TripleID;
import org.roaringbitmap.*;
import reasoning.saturation.distributed.metadata.ControlNodeStatistics;
import reasoning.saturation.distributed.metadata.DistributedSaturationConfiguration;
import reasoning.saturation.distributed.metadata.SaturationConfiguration;
import reasoning.saturation.distributed.metadata.WorkerStatistics;
import reasoning.saturation.models.DistributedWorkerModel;
import reasoning.saturation.models.WorkerModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class KryoUtils {

    public static Stream<Class<?>> getClasses() {
        Stream.Builder<Class<?>> sb = Stream.builder();

        // general
        sb.add(UnifiedSet.class);
        sb.add(ArrayList.class);
        sb.add(HashMap.class);
        sb.add(char[].class);
        sb.add(long[].class);
        sb.add(int[].class);
        sb.add(AtomicLong.class);
        sb.add(AtomicInteger.class);
        sb.add(AtomicBoolean.class);

        // RoaringBitmap
        sb.add(RoaringBitmap.class);
        sb.add(RoaringArray.class);
        sb.add(Container[].class);
        sb.add(ArrayContainer.class);
        sb.add(BitmapContainer.class);

        // echo
        sb.add(EchoAxiom.class);
        sb.add(EchoAxiomA.class);
        sb.add(EchoAxiomB.class);
        sb.add(EchoAToBRule.class);
        sb.add(EchoBToARule.class);
        sb.add(EchoWorkloadDistributor.class);
        sb.add(EchoClosure.class);

        // transitive closure
        sb.add(Reachability.class);
        sb.add(ToldReachability.class);
        sb.add(DerivedReachability.class);
        sb.add(InitRule.class);
        sb.add(DeriveReachabilityRule.class);
        sb.add(ReachabilityWorkloadDistributor.class);
        sb.add(ReachabilityClosure.class);

        // RDFS reasoning
        sb.add(TripleID.class);
        sb.add(RDFSClosure.class);
        sb.add(RuleRDFS1.class);
        sb.add(RuleRDFS2.class);
        sb.add(RuleRDFS3.class);
        sb.add(RuleRDFS4a.class);
        sb.add(RuleRDFS4b.class);
        sb.add(RuleRDFS5.class);
        sb.add(RuleRDFS6.class);
        sb.add(RuleRDFS7.class);
        sb.add(RuleRDFS8.class);
        sb.add(RuleRDFS9.class);
        sb.add(RuleRDFS10.class);
        sb.add(RuleRDFS11.class);
        sb.add(RuleRDFS12.class);
        sb.add(RuleRDFS13.class);
        sb.add(RuleGRDFD1.class);
        sb.add(RuleGRDFD2.class);

        // state messages
        sb.add(AcknowledgementMessage.class);
        sb.add(AxiomCount.class);
        sb.add(DebugMessage.class);
        sb.add(InitializeWorkerMessage.class);
        sb.add(MessageEnvelope.class);
        sb.add(StateInfoMessage.class);
        sb.add(StatisticsMessage.class);
        sb.add(RequestAxiomMessageCount.class);
        sb.add(MessageModel.class);
        sb.add(enums.SaturationStatusMessage.class);
        sb.add(enums.SaturationApproach.class);
        sb.add(enums.StatisticsComponent.class);
        sb.add(enums.NetworkingComponentType.class);

        // saturation classes
        sb.add(SaturationConfiguration.class);
        sb.add(DistributedSaturationConfiguration.class);
        sb.add(MessageDistributionType.class);
        sb.add(WorkerStatistics.class);
        sb.add(ControlNodeStatistics.class);
        sb.add(WorkerModel.class);
        sb.add(DistributedWorkerModel.class);

        // networking
        sb.add(ServerData.class);

        // other
        sb.add(Edge.class);
        sb.add(TestObject.class);

        return sb.build();
    }
}
