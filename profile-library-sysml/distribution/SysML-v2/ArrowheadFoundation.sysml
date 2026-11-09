library package ArrowheadFoundation {
    private import ScalarValues::*;
    private import ArrowheadProfile::*;
    item def JSONValue { doc /* An unconstrained JSON value; used where the source has an open map or unknown schema. */ }
    item def JSONMessage :> JSONValue;
    abstract part def ArrowheadSystem {
        attribute implementationVersion : String = "5.2.2";
        attribute releaseStatus : String = "RELEASE CANDIDATE";
    }
    abstract port def ArrowheadService;
    abstract part def Device;
    abstract part def IntraCloudNetwork;
    abstract part def InterCloudNetwork;
    abstract part def LocalCloud { part systems : ArrowheadSystem[0..*]; part devices : Device[0..*]; }
    abstract part def SystemOfLocalClouds { part clouds : LocalCloud[0..*]; }
    connection def DeviceNetworkConnection {
        end device : Device;
        end network : IntraCloudNetwork;
    }
    connection def InterCloudDeviceNetworkConnection {
        end device : Device;
        end network : InterCloudNetwork;
    }
    connection def LocalCloudDesign_System_of_LocalcloudsDesign { end leftEnd : LocalCloud; end rightEnd : SystemOfLocalClouds; }
    connection def System_of_LocacloudsDesignDescription_LocalcloudDesignDescription { end leftEnd : SystemOfLocalClouds; end rightEnd : LocalCloud; }
    connection def System_of_LocalcloudsImplementation_LocalcloudImplementation { end leftEnd : SystemOfLocalClouds; end rightEnd : LocalCloud; }
    connection def LocalcloudImplementation_System_Implementation { end leftEnd : LocalCloud; end rightEnd : ArrowheadSystem; }
    connection def DeviceDesign_IntracloudNetworkDesign { end leftEnd : Device; end rightEnd : IntraCloudNetwork; }
    connection def DeviceDesign_IntercloudNetworkDesign { end leftEnd : Device; end rightEnd : InterCloudNetwork; }
    connection def DeviceDesignDescription_IntracloudNetworkDesignDescription { end leftEnd : Device; end rightEnd : IntraCloudNetwork; }
    connection def DeviceDesignDescription_IntercloudNetworkDesignDescription { end leftEnd : Device; end rightEnd : InterCloudNetwork; }
    connection def System_Implementation_IDD_Implementation { end system : ArrowheadSystem; end port service : ArrowheadService; }
}
