library package ArrowheadLocalCloudLibrary {
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    part def ArrowheadLocalCloud :> LocalCloud {
        @LocalcloudImplementation;
        part 'serviceRegistry' : ArrowheadServiceRegistry::ServiceRegistry[0..1] :> systems;
        part 'authentication' : ArrowheadAuthentication::Authentication[0..1] :> systems;
        part 'consumerAuthorization' : ArrowheadConsumerAuthorization::ConsumerAuthorization[0..1] :> systems;
        part 'dynamicServiceOrchestration' : ArrowheadDynamicServiceOrchestration::DynamicServiceOrchestration[0..1] :> systems;
        part 'simpleStoreServiceOrchestration' : ArrowheadSimpleStoreServiceOrchestration::SimpleStoreServiceOrchestration[0..1] :> systems;
        part 'aIGym' : ArrowheadAIGym::AIGym[0..1] :> systems;
        part 'blacklist' : ArrowheadBlacklist::Blacklist[0..1] :> systems;
        part 'certificateAuthority' : ArrowheadCertificateAuthority::CertificateAuthority[0..1] :> systems;
        part 'choreographer' : ArrowheadChoreographer::Choreographer[0..1] :> systems;
        part 'configuration' : ArrowheadConfiguration::Configuration[0..1] :> systems;
        part 'dataManager' : ArrowheadDataManager::DataManager[0..1] :> systems;
        part 'deviceQoSEvaluator' : ArrowheadDeviceQoSEvaluator::DeviceQoSEvaluator[0..1] :> systems;
        part 'eventHandler' : ArrowheadEventHandler::EventHandler[0..1] :> systems;
        part 'gateTunnel' : ArrowheadGateTunnel::GateTunnel[0..1] :> systems;
        part 'gatekeeper' : ArrowheadGatekeeper::Gatekeeper[0..1] :> systems;
        part 'localCloudManager' : ArrowheadLocalCloudManager::LocalCloudManager[0..1] :> systems;
        part 'onboarding' : ArrowheadOnboarding::Onboarding[0..1] :> systems;
        part 'plantDescriptionEngine' : ArrowheadPlantDescriptionEngine::PlantDescriptionEngine[0..1] :> systems;
        part 'sysMonitor' : ArrowheadSysMonitor::SysMonitor[0..1] :> systems;
        part 'timeManager' : ArrowheadTimeManager::TimeManager[0..1] :> systems;
        part 'translationManager' : ArrowheadTranslationManager::TranslationManager[0..1] :> systems;
    }
    part def ArrowheadSystemOfLocalClouds :> SystemOfLocalClouds {
        @System_of_LocalcloudsImplementation;
        part localClouds : ArrowheadLocalCloud[0..*] :> clouds;
    }
}
