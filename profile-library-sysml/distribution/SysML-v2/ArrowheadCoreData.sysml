library package ArrowheadCoreData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    enum def 'AddressType' {
        enum 'HOSTNAME';
        enum 'IPV4';
        enum 'IPV6';
        enum 'MAC';
    }
    enum def 'AuthenticationMethod' {
        enum 'PASSWORD';
    }
    enum def 'AuthorizationLevel' {
        enum 'MGMT';
        enum 'PROVIDER';
    }
    enum def 'AuthorizationPolicyType' {
        enum 'ALL';
        enum 'WHITELIST';
        enum 'BLACKLIST';
        enum 'SYS_METADATA';
    }
    enum def 'AuthorizationTargetType' {
        enum 'SERVICE_DEF';
        enum 'EVENT_TYPE';
    }
    enum def 'AuthorizationTokenType' {
        enum 'TIME_LIMITED_TOKEN';
        enum 'USAGE_LIMITED_TOKEN';
        enum 'SELF_CONTAINED_TOKEN';
        enum 'TRANSLATION_BRIDGE_TOKEN';
    }
    enum def 'DataModelTranslationTaskStatus' {
        enum 'PENDING';
        enum 'IN_PROGRESS';
        enum 'DONE';
        enum 'ERROR';
    }
    enum def 'ExceptionType' {
        enum 'ARROWHEAD';
        enum 'INVALID_PARAMETER';
        enum 'AUTH';
        enum 'FORBIDDEN';
        enum 'DATA_NOT_FOUND';
        enum 'TIMEOUT';
        enum 'LOCKED';
        enum 'INTERNAL_SERVER_ERROR';
        enum 'EXTERNAL_SERVER_ERROR';
    }
    enum def 'Mode' {
        enum 'ALL';
        enum 'ACTIVES';
        enum 'INACTIVES';
    }
    enum def 'NotifyProtocol' {
        enum 'HTTP';
        enum 'HTTPS';
        enum 'MQTT';
        enum 'MQTTS';
    }
    enum def 'OrchestrationFlag' {
        enum 'MATCHMAKING';
        enum 'ONLY_PREFERRED';
        enum 'ONLY_EXCLUSIVE';
        enum 'ALLOW_TRANSLATION';
        enum 'ALLOW_INTERCLOUD';
        enum 'ONLY_INTERCLOUD';
    }
    enum def 'OrchestrationType' {
        enum 'PULL';
        enum 'PUSH';
    }
    enum def 'QoSOperation' {
        enum 'FILTER';
        enum 'SORT';
    }
    enum def 'ServiceInterfacePolicy' {
        enum 'NONE';
        enum 'CERT_AUTH';
        enum 'TIME_LIMITED_TOKEN_AUTH';
        enum 'USAGE_LIMITED_TOKEN_AUTH';
        enum 'BASE64_SELF_CONTAINED_TOKEN_AUTH';
        enum 'RSA_SHA256_JSON_WEB_TOKEN_AUTH';
        enum 'RSA_SHA512_JSON_WEB_TOKEN_AUTH';
        enum 'TRANSLATION_BRIDGE_TOKEN_AUTH';
    }
    enum def 'TranslationBridgeEventState' {
        enum 'USED';
        enum 'INTERNAL_CLOSED';
        enum 'INTERNAL_ERROR';
        enum 'EXTERNAL_ERROR';
    }
    enum def 'TranslationBridgeStatus' {
        enum 'NEW';
        enum 'DISCOVERED';
        enum 'PENDING';
        enum 'INITIALIZED';
        enum 'USED';
        enum 'ABORTED';
        enum 'CLOSED';
        enum 'ERROR';
    }
    enum def 'TranslationDiscoveryFlag' {
        enum 'CONSUMER_BLACKLIST_CHECK';
        enum 'CANDIDATES_BLACKLIST_CHECK';
        enum 'TRANSLATORS_BLACKLIST_CHECK';
        enum 'CANDIDATES_AUTH_CHECK';
        enum 'TRANSLATORS_AUTH_CHECK';
    }
    item def 'AddressDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'type' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'address' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationEncryptionKeyRegistrationRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'key' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'algorithm' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationGrantRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'cloud' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetType' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'target' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'description' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'defaultPolicy' : 'AuthorizationPolicyRequestDTO'[0..1] { doc /* Source type: AuthorizationPolicyRequestDTO.  */ }
        ref item 'scopedPolicies' : JSONValue[0..1] { doc /* Source type: Map<String, AuthorizationPolicyRequestDTO>.  */ }
    }
    item def 'AuthorizationLookupRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'instanceIds' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'cloudIdentifiers' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'targetNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'targetType' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationMgmtEncryptionKeyListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'AuthorizationMgmtEncryptionKeyResponseDTO'[0..*] { doc /* Source type: List<AuthorizationMgmtEncryptionKeyResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'list' : 'AuthorizationMgmtEncryptionKeyRegistrationRequestDTO'[0..*] { doc /* Source type: List<AuthorizationMgmtEncryptionKeyRegistrationRequestDTO>.  */ }
    }
    item def 'AuthorizationMgmtEncryptionKeyRegistrationRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'key' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'algorithm' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationMgmtEncryptionKeyResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'rawKey' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'algorithm' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'keyAdditive' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationMgmtGrantListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'list' : 'AuthorizationMgmtGrantRequestDTO'[0..*] { doc /* Source type: List<AuthorizationMgmtGrantRequestDTO>.  */ }
    }
    item def 'AuthorizationMgmtGrantRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'cloud' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'provider' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetType' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'target' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'description' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'defaultPolicy' : 'AuthorizationPolicyRequestDTO'[0..1] { doc /* Source type: AuthorizationPolicyRequestDTO.  */ }
        ref item 'scopedPolicies' : JSONValue[0..1] { doc /* Source type: Map<String, AuthorizationPolicyRequestDTO>.  */ }
    }
    item def 'AuthorizationPolicyDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'policyType' : 'AuthorizationPolicyType'[0..1] { doc /* Source type: AuthorizationPolicyType.  */ }
        attribute 'policyList' : String[0..*] { doc /* Source type: List<String>.  */ }
        ref item 'policyMetadataRequirement' : JSONValue[0..1] { doc /* Source type: MetadataRequirementDTO.  */ }
    }
    item def 'AuthorizationPolicyListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'AuthorizationPolicyResponseDTO'[0..*] { doc /* Source type: List<AuthorizationPolicyResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'AuthorizationPolicyRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'policyType' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'policyList' : String[0..*] { doc /* Source type: List<String>.  */ }
        ref item 'policyMetadataRequirement' : JSONValue[0..1] { doc /* Source type: MetadataRequirementDTO.  */ }
    }
    item def 'AuthorizationPolicyResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'instanceId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'level' : 'AuthorizationLevel'[0..1] { doc /* Source type: AuthorizationLevel.  */ }
        attribute 'cloud' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'provider' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetType' : 'AuthorizationTargetType'[0..1] { doc /* Source type: AuthorizationTargetType.  */ }
        attribute 'target' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'description' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'defaultPolicy' : 'AuthorizationPolicyDTO'[0..1] { doc /* Source type: AuthorizationPolicyDTO.  */ }
        ref item 'scopedPolicies' : JSONValue[0..1] { doc /* Source type: Map<String, AuthorizationPolicyDTO>.  */ }
        attribute 'createdBy' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'level' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'providers' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'instanceIds' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'cloudIdentifiers' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'targetNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'targetType' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationTokenGenerationMgmtListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'list' : 'AuthorizationTokenGenerationMgmtRequestDTO'[0..*] { doc /* Source type: List<AuthorizationTokenGenerationMgmtRequestDTO>.  */ }
    }
    item def 'AuthorizationTokenGenerationMgmtRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'tokenVariant' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetType' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'consumerCloud' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'provider' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'target' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'scope' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expiresAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'usageLimit' : Integer[0..1] { doc /* Source type: Integer.  */ }
    }
    item def 'AuthorizationTokenGenerationRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'tokenVariant' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'provider' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetType' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'target' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'scope' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationTokenGenerationResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'tokenType' : 'AuthorizationTokenType'[0..1] { doc /* Source type: AuthorizationTokenType.  */ }
        attribute 'targetType' : 'AuthorizationTargetType'[0..1] { doc /* Source type: AuthorizationTargetType.  */ }
        attribute 'token' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'usageLimit' : Integer[0..1] { doc /* Source type: Integer.  */ }
        attribute 'expiresAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationTokenMgmtListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'AuthorizationTokenResponseDTO'[0..*] { doc /* Source type: List<AuthorizationTokenResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'AuthorizationTokenQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'requester' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'tokenType' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'consumerCloud' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'provider' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetType' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'target' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationTokenResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'tokenType' : 'AuthorizationTokenType'[0..1] { doc /* Source type: AuthorizationTokenType.  */ }
        attribute 'variant' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'token' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'tokenReference' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'requester' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'consumerCloud' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'provider' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetType' : 'AuthorizationTargetType'[0..1] { doc /* Source type: AuthorizationTargetType.  */ }
        attribute 'target' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'scope' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'usageLimit' : Integer[0..1] { doc /* Source type: Integer.  */ }
        attribute 'usageLeft' : Integer[0..1] { doc /* Source type: Integer.  */ }
        attribute 'expiresAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationTokenVerifyResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'verified' : Boolean[0..1] { doc /* Source type: boolean.  */ }
        attribute 'consumerCloud' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetType' : 'AuthorizationTargetType'[0..1] { doc /* Source type: AuthorizationTargetType.  */ }
        attribute 'target' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'scope' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationVerifyListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'list' : 'AuthorizationVerifyRequestDTO'[0..*] { doc /* Source type: List<AuthorizationVerifyRequestDTO>.  */ }
    }
    item def 'AuthorizationVerifyListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'AuthorizationVerifyResponseDTO'[0..*] { doc /* Source type: List<AuthorizationVerifyResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'AuthorizationVerifyRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'provider' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'cloud' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetType' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'target' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'scope' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'AuthorizationVerifyResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'provider' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'cloud' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetType' : 'AuthorizationTargetType'[0..1] { doc /* Source type: AuthorizationTargetType.  */ }
        attribute 'target' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'scope' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'granted' : Boolean[0..1] { doc /* Source type: boolean.  */ }
    }
    item def 'BlacklistCreateListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entities' : 'BlacklistCreateRequestDTO'[0..*] { doc /* Source type: List<BlacklistCreateRequestDTO>.  */ }
    }
    item def 'BlacklistCreateRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expiresAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'reason' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'BlacklistEntryDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'createdBy' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'revokedBy' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'updatedAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'reason' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expiresAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'active' : Boolean[0..1] { doc /* Source type: boolean.  */ }
    }
    item def 'BlacklistEntryListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'BlacklistEntryDTO'[0..*] { doc /* Source type: List<BlacklistEntryDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'BlacklistQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'systemNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'mode' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'issuers' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'revokers' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'reason' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'alivesAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'ConfigListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'names' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'DataModelFactoryTranslatorInitiaizationResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'dataModelTranslatorName' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'interfaceProperties' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
    }
    item def 'DataModelTranslationInitRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'inputModelId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'outputModelId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'payload' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'settings' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
    }
    item def 'DataModelTranslationResultResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'status' : 'DataModelTranslationTaskStatus'[0..1] { doc /* Source type: DataModelTranslationTaskStatus.  */ }
        attribute 'result' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'mimeType' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'DataModelTranslatorFactoryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'fromModelId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'toModelId' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'DeviceListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'devices' : 'DeviceRequestDTO'[0..*] { doc /* Source type: List<DeviceRequestDTO>.  */ }
    }
    item def 'DeviceListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'DeviceResponseDTO'[0..*] { doc /* Source type: List<DeviceResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'DeviceLookupRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'deviceNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'addresses' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'addressType' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadataRequirementList' : JSONValue[0..*] { doc /* Source type: List<MetadataRequirementDTO>.  */ }
    }
    item def 'DeviceQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'deviceNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'addresses' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'addressType' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadataRequirementList' : JSONValue[0..*] { doc /* Source type: List<MetadataRequirementDTO>.  */ }
    }
    item def 'DeviceRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        attribute 'addresses' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'DeviceResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        ref item 'addresses' : 'AddressDTO'[0..*] { doc /* Source type: List<AddressDTO>.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'updatedAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'ErrorMessageDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'errorMessage' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'errorCode' : Integer[0..1] { doc /* Source type: int.  */ }
        attribute 'exceptionType' : 'ExceptionType'[0..1] { doc /* Source type: ExceptionType.  */ }
        attribute 'origin' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'IdentityChangeRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'credentials' : JSONValue[0..1] { doc /* Source type: Map<String, String>.  */ }
        ref item 'newCredentials' : JSONValue[0..1] { doc /* Source type: Map<String, String>.  */ }
    }
    item def 'IdentityListMgmtCreateRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'authenticationMethod' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'identities' : 'IdentityMgmtRequestDTO'[0..*] { doc /* Source type: List<IdentityMgmtRequestDTO>.  */ }
    }
    item def 'IdentityListMgmtResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'identities' : 'IdentityMgmtResponseDTO'[0..*] { doc /* Source type: List<IdentityMgmtResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'IdentityListMgmtUpdateRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'identities' : 'IdentityMgmtRequestDTO'[0..*] { doc /* Source type: List<IdentityMgmtRequestDTO>.  */ }
    }
    item def 'IdentityLoginResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'token' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expirationTime' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'IdentityMgmtRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'credentials' : JSONValue[0..1] { doc /* Source type: Map<String, String>.  */ }
        attribute 'sysop' : Boolean[0..1] { doc /* Source type: Boolean.  */ }
    }
    item def 'IdentityMgmtResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'authenticationMethod' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'sysop' : Boolean[0..1] { doc /* Source type: boolean.  */ }
        attribute 'createdBy' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'updatedBy' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'updatedAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'IdentityQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'namePart' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'isSysop' : Boolean[0..1] { doc /* Source type: Boolean.  */ }
        attribute 'createdBy' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'creationFrom' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'creationTo' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'hasSession' : Boolean[0..1] { doc /* Source type: Boolean.  */ }
    }
    item def 'IdentityRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'credentials' : JSONValue[0..1] { doc /* Source type: Map<String, String>.  */ }
    }
    item def 'IdentitySessionListMgmtResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'sessions' : 'IdentitySessionResponseDTO'[0..*] { doc /* Source type: List<IdentitySessionResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'IdentitySessionQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'namePart' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'loginFrom' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'loginTo' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'IdentitySessionResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'loginTime' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expirationTime' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'IdentityVerifyResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'verified' : Boolean[0..1] { doc /* Source type: boolean.  */ }
        attribute 'systemName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'sysop' : Boolean[0..1] { doc /* Source type: Boolean.  */ }
        attribute 'loginTime' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expirationTime' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'KeyValuesDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'map' : JSONValue[0..1] { doc /* Source type: Map<String, String>.  */ }
    }
    item def 'LogEntryDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'logId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'entryDate' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'message' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'exception' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'LogEntryListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'LogEntryDTO'[0..*] { doc /* Source type: List<LogEntryDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'LogRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'from' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'to' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'MqttNotifyTemplate' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'receiver' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'sender' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'payload' : JSONValue[0..1] { doc /* Source type: Object.  */ }
    }
    item def 'MqttPublishTemplate' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'sender' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'payload' : JSONValue[0..1] { doc /* Source type: Object.  */ }
    }
    item def 'MqttRequestTemplate' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'traceId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'authentication' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'responseTopic' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'qosRequirement' : Integer[0..1] { doc /* Source type: Integer.  */ }
        ref item 'params' : JSONValue[0..1] { doc /* Source type: Map<String, String>.  */ }
        ref item 'payload' : JSONValue[0..1] { doc /* Source type: Object.  */ }
    }
    item def 'MqttResponseTemplate' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'status' : Integer[0..1] { doc /* Source type: int.  */ }
        attribute 'traceId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'receiver' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'payload' : JSONValue[0..1] { doc /* Source type: Object.  */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'schemaVersion' : String[0..1] { doc /* Source type: String. 1.0. */ }
        attribute 'observationId' : String[0..1] { doc /* Source type: String. <System>-<type>-<epoch ms>-<sequence>. */ }
        attribute 'providerSystem' : String[0..1] { doc /* Source type: String. The publishing system. */ }
        attribute 'serviceInstance' : String[0..1] { doc /* Source type: String. Service instance, default the system name. */ }
        attribute 'observationType' : String[0..1] { doc /* Source type: String. echo, power or status. */ }
        attribute 'sourceTime' : String[0..1] { doc /* Source type: String. ISO 8601 UTC time with milliseconds. */ }
        attribute 'sequence' : Real[0..1] { doc /* Source type: Number. Sequence number per publisher. */ }
        ref item 'value' : JSONValue[0..1] { doc /* Source type: Object. Observed value, see the operations. */ }
        attribute 'unit' : String[0..1] { doc /* Source type: String. 1 for power and status; absent for echo. */ }
        attribute 'quality' : String[0..1] { doc /* Source type: String. good or uncertain. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. \texttt{{"publisherMode": "native"}}. */ }
    }
    item def 'OrchestrationHistoryQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'ids' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'statuses' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'type' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'requesterSystems' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'targetSystems' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'serviceDefinitions' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'subscriptionIds' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'OrchestrationHistoryResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'OrchestrationJobDTO'[0..*] { doc /* Source type: List<OrchestrationJobDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'OrchestrationJobDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'status' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'type' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'requesterSystem' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetSystem' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'subscriptionId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'message' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'startedAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'finishedAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'OrchestrationLockListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'locks' : 'OrchestrationLockRequestDTO'[0..*] { doc /* Source type: List<OrchestrationLockRequestDTO>.  */ }
    }
    item def 'OrchestrationLockListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'OrchestrationLockResponseDTO'[0..*] { doc /* Source type: List<OrchestrationLockResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'OrchestrationLockQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'ids' : Integer[0..*] { doc /* Source type: List<Long>.  */ }
        attribute 'orchestrationJobIds' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'serviceInstanceIds' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'owners' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'expiresBefore' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expiresAfter' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'OrchestrationLockRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'serviceInstanceId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'owner' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expiresAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'OrchestrationLockResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Integer[0..1] { doc /* Source type: long.  */ }
        attribute 'orchestrationJobId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'serviceInstanceId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'owner' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expiresAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'temporary' : Boolean[0..1] { doc /* Source type: boolean.  */ }
    }
    item def 'OrchestrationNotifyInterfaceDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'protocol' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'properties' : JSONValue[0..1] { doc /* Source type: Map<String, String>.  */ }
    }
    item def 'OrchestrationPlantProviderDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'serviceInstanceId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'priority' : Integer[0..1] { doc /* Source type: Integer.  */ }
    }
    item def 'OrchestrationPlantRuleDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'consumer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'providers' : 'OrchestrationPlantProviderDTO'[0..*] { doc /* Source type: List<OrchestrationPlantProviderDTO>.  */ }
    }
    item def 'OrchestrationPlantRulesDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'rules' : 'OrchestrationPlantRuleDTO'[0..*] { doc /* Source type: List<OrchestrationPlantRuleDTO>.  */ }
    }
    item def 'OrchestrationPushJobListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'jobs' : 'OrchestrationJobDTO'[0..*] { doc /* Source type: List<OrchestrationJobDTO>.  */ }
    }
    item def 'OrchestrationPushTriggerDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'targetSystems' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'subscriptionIds' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'OrchestrationRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'serviceRequirement' : 'OrchestrationServiceRequirementDTO'[0..1] { doc /* Source type: OrchestrationServiceRequirementDTO.  */ }
        ref item 'orchestrationFlags' : JSONValue[0..1] { doc /* Source type: Map<String, Boolean>.  */ }
        ref item 'qualityRequirements' : 'QoSRequirementDTO'[0..*] { doc /* Source type: List<QoSRequirementDTO>.  */ }
        attribute 'exclusivityDuration' : Integer[0..1] { doc /* Source type: Integer.  */ }
    }
    item def 'OrchestrationResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'results' : 'OrchestrationResultDTO'[0..*] { doc /* Source type: List<OrchestrationResultDTO>.  */ }
        attribute 'warnings' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'OrchestrationResultDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'serviceInstanceId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'cloudIdentitifer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'providerName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'serviceDefinitition' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'version' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'aliveUntil' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'exclusiveUntil' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        ref item 'interfaces' : 'ServiceInstanceInterfaceResponseDTO'[0..*] { doc /* Source type: List<ServiceInstanceInterfaceResponseDTO>.  */ }
        ref item 'authorizationTokens' : JSONValue[0..1] { doc /* Source type: Map<String, Map<String, AuthorizationTokenGenerationResponseDTO>>.  */ }
    }
    item def 'OrchestrationServiceRequirementDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'operations' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'versions' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'alivesAt' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadataRequirements' : JSONValue[0..*] { doc /* Source type: List<MetadataRequirementDTO>.  */ }
        attribute 'interfaceTemplateNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'interfaceAddressTypes' : String[0..*] { doc /* Source type: List<String>.  */ }
        ref item 'interfacePropertyRequirements' : JSONValue[0..*] { doc /* Source type: List<MetadataRequirementDTO>.  */ }
        attribute 'securityPolicies' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'preferredProviders' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'OrchestrationSimpleStoreListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'candidates' : 'OrchestrationSimpleStoreRequestDTO'[0..*] { doc /* Source type: List<OrchestrationSimpleStoreRequestDTO>.  */ }
    }
    item def 'OrchestrationSimpleStoreListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'OrchestrationSimpleStoreResponseDTO'[0..*] { doc /* Source type: List<OrchestrationSimpleStoreResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'OrchestrationSimpleStoreQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'ids' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'consumerNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'serviceDefinitions' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'serviceInstanceIds' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'minPriority' : Integer[0..1] { doc /* Source type: Integer.  */ }
        attribute 'maxPriority' : Integer[0..1] { doc /* Source type: Integer.  */ }
        attribute 'createdBy' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'OrchestrationSimpleStoreRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'consumer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'serviceInstanceId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'priority' : Integer[0..1] { doc /* Source type: Integer.  */ }
    }
    item def 'OrchestrationSimpleStoreResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'serviceInstanceId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'priority' : Integer[0..1] { doc /* Source type: Integer.  */ }
        attribute 'createdBy' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'updatedBy' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'updatedAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'OrchestrationSubscriptionListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'subscriptions' : 'OrchestrationSubscriptionRequestDTO'[0..*] { doc /* Source type: List<OrchestrationSubscriptionRequestDTO>.  */ }
    }
    item def 'OrchestrationSubscriptionListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'OrchestrationSubscriptionResponseDTO'[0..*] { doc /* Source type: List<OrchestrationSubscriptionResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'OrchestrationSubscriptionQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'ownerSystems' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'targetSystems' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'serviceDefinitions' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'OrchestrationSubscriptionRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'targetSystemName' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'orchestrationRequest' : 'OrchestrationRequestDTO'[0..1] { doc /* Source type: OrchestrationRequestDTO.  */ }
        ref item 'notifyInterface' : 'OrchestrationNotifyInterfaceDTO'[0..1] { doc /* Source type: OrchestrationNotifyInterfaceDTO.  */ }
        attribute 'duration' : Integer[0..1] { doc /* Source type: Long.  */ }
    }
    item def 'OrchestrationSubscriptionResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'ownerSystemName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetSystemName' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'orchestrationRequest' : 'OrchestrationRequestDTO'[0..1] { doc /* Source type: OrchestrationRequestDTO.  */ }
        ref item 'notifyInterface' : 'OrchestrationNotifyInterfaceDTO'[0..1] { doc /* Source type: OrchestrationNotifyInterfaceDTO.  */ }
        attribute 'expiredAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'PageDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'page' : Integer[0..1] { doc /* Source type: Integer.  */ }
        attribute 'size' : Integer[0..1] { doc /* Source type: Integer.  */ }
        attribute 'direction' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'sortField' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'QoSDeviceDataEvaluationConfigDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'metricNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'metricWeights' : Real[0..*] { doc /* Source type: List<Double>.  */ }
        attribute 'timeWindow' : Integer[0..1] { doc /* Source type: Integer.  */ }
        attribute 'threshold' : Real[0..1] { doc /* Source type: Double.  */ }
    }
    item def 'QoSDeviceStatQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'metricGroup' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'from' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'to' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'aggregation' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'systemNames' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'QoSDeviceStatQueryResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'records' : 'QoSDeviceStatRecordDTO'[0..*] { doc /* Source type: List<QoSDeviceStatRecordDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'QoSDeviceStatRecordDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'metricGroup' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'id' : Integer[0..1] { doc /* Source type: long.  */ }
        attribute 'timestamp' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'uuid' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'minimum' : Real[0..1] { doc /* Source type: Double.  */ }
        attribute 'maximum' : Real[0..1] { doc /* Source type: Double.  */ }
        attribute 'mean' : Real[0..1] { doc /* Source type: Double.  */ }
        attribute 'median' : Real[0..1] { doc /* Source type: Double.  */ }
        attribute 'current' : Real[0..1] { doc /* Source type: Double.  */ }
        attribute 'systems' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'QoSEvaluationFilterResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'passedProviders' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'droppedProviders' : String[0..*] { doc /* Source type: List<String>.  */ }
        ref item 'warnings' : JSONValue[0..1] { doc /* Source type: Map<String, List<String>>.  */ }
    }
    item def 'QoSEvaluationRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'providers' : String[0..*] { doc /* Source type: List<String>.  */ }
        ref item 'configuration' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
    }
    item def 'QoSEvaluationSortResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'sortedProviders' : String[0..*] { doc /* Source type: List<String>.  */ }
        ref item 'warnings' : JSONValue[0..1] { doc /* Source type: Map<String, List<String>>.  */ }
    }
    item def 'QoSRequirementDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'type' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'operation' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'requirements' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
    }
    item def 'ServiceDefinitionListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'serviceDefinitionNames' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'ServiceDefinitionListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'ServiceDefinitionResponseDTO'[0..*] { doc /* Source type: List<ServiceDefinitionResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'ServiceDefinitionResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'updatedAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'ServiceInstanceCreateListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'instances' : 'ServiceInstanceRequestDTO'[0..*] { doc /* Source type: List<ServiceInstanceRequestDTO>.  */ }
    }
    item def 'ServiceInstanceCreateRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'serviceDefinitionName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'version' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expiresAt' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        ref item 'interfaces' : 'ServiceInstanceInterfaceRequestDTO'[0..*] { doc /* Source type: List<ServiceInstanceInterfaceRequestDTO>.  */ }
    }
    item def 'ServiceInstanceInterfaceRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'templateName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'protocol' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'policy' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'properties' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
    }
    item def 'ServiceInstanceInterfaceResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'templateName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'protocol' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'policy' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'properties' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
    }
    item def 'ServiceInstanceListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'ServiceInstanceResponseDTO'[0..*] { doc /* Source type: List<ServiceInstanceResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'ServiceInstanceLookupRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'instanceIds' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'providerNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'serviceDefinitionNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'versions' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'alivesAt' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadataRequirementsList' : JSONValue[0..*] { doc /* Source type: List<MetadataRequirementDTO>.  */ }
        attribute 'addressTypes' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'interfaceTemplateNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        ref item 'interfacePropertyRequirementsList' : JSONValue[0..*] { doc /* Source type: List<MetadataRequirementDTO>.  */ }
        attribute 'policies' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'ServiceInstanceQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'instanceIds' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'providerNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'serviceDefinitionNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'versions' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'alivesAt' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadataRequirementsList' : JSONValue[0..*] { doc /* Source type: List<MetadataRequirementDTO>.  */ }
        attribute 'addressTypes' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'interfaceTemplateNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        ref item 'interfacePropertyRequirementsList' : JSONValue[0..*] { doc /* Source type: List<MetadataRequirementDTO>.  */ }
        attribute 'policies' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'ServiceInstanceRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'serviceDefinitionName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'version' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expiresAt' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        ref item 'interfaces' : 'ServiceInstanceInterfaceRequestDTO'[0..*] { doc /* Source type: List<ServiceInstanceInterfaceRequestDTO>.  */ }
    }
    item def 'ServiceInstanceResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'instanceId' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'provider' : 'SystemResponseDTO'[0..1] { doc /* Source type: SystemResponseDTO.  */ }
        ref item 'serviceDefinition' : 'ServiceDefinitionResponseDTO'[0..1] { doc /* Source type: ServiceDefinitionResponseDTO.  */ }
        attribute 'version' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expiresAt' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        ref item 'interfaces' : 'ServiceInstanceInterfaceResponseDTO'[0..*] { doc /* Source type: List<ServiceInstanceInterfaceResponseDTO>.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'updatedAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'ServiceInstanceUpdateListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'instances' : 'ServiceInstanceUpdateRequestDTO'[0..*] { doc /* Source type: List<ServiceInstanceUpdateRequestDTO>.  */ }
    }
    item def 'ServiceInstanceUpdateRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'instanceId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'expiresAt' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        ref item 'interfaces' : 'ServiceInstanceInterfaceRequestDTO'[0..*] { doc /* Source type: List<ServiceInstanceInterfaceRequestDTO>.  */ }
    }
    item def 'ServiceInterfaceTemplateListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'interfaceTemplates' : 'ServiceInterfaceTemplateRequestDTO'[0..*] { doc /* Source type: List<ServiceInterfaceTemplateRequestDTO>.  */ }
    }
    item def 'ServiceInterfaceTemplateListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'ServiceInterfaceTemplateResponseDTO'[0..*] { doc /* Source type: List<ServiceInterfaceTemplateResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'ServiceInterfaceTemplatePropertyDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'mandatory' : Boolean[0..1] { doc /* Source type: boolean.  */ }
        attribute 'validator' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'validatorParams' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'ServiceInterfaceTemplateQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'templateNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'protocols' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'ServiceInterfaceTemplateRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'protocol' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'propertyRequirements' : 'ServiceInterfaceTemplatePropertyDTO'[0..*] { doc /* Source type: List<ServiceInterfaceTemplatePropertyDTO>.  */ }
    }
    item def 'ServiceInterfaceTemplateResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'protocol' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'propertyRequirements' : 'ServiceInterfaceTemplatePropertyDTO'[0..*] { doc /* Source type: List<ServiceInterfaceTemplatePropertyDTO>.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'updatedAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'SystemListRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'systems' : 'SystemRequestDTO'[0..*] { doc /* Source type: List<SystemRequestDTO>.  */ }
    }
    item def 'SystemListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'SystemResponseDTO'[0..*] { doc /* Source type: List<SystemResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'SystemLookupRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'addresses' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'addressType' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadataRequirementList' : JSONValue[0..*] { doc /* Source type: List<MetadataRequirementDTO>.  */ }
        attribute 'versions' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'deviceNames' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'SystemQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'systemNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'addresses' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'addressType' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadataRequirementList' : JSONValue[0..*] { doc /* Source type: List<MetadataRequirementDTO>.  */ }
        attribute 'versions' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'deviceNames' : String[0..*] { doc /* Source type: List<String>.  */ }
    }
    item def 'SystemRegisterRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        attribute 'version' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'addresses' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'deviceName' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'SystemRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        attribute 'version' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'addresses' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'deviceName' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'SystemResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        attribute 'version' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'addresses' : 'AddressDTO'[0..*] { doc /* Source type: List<AddressDTO>.  */ }
        ref item 'device' : 'DeviceResponseDTO'[0..1] { doc /* Source type: DeviceResponseDTO.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'updatedAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'TranslationAbortMgmtResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'results' : JSONValue[0..1] { doc /* Source type: Map<String, Boolean>.  */ }
    }
    item def 'TranslationBridgeCandidateDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'serviceInstanceId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'interfaceTemplateName' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'TranslationBridgeInitializationRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'inputInterface' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'inputDataModelTranslator' : 'TranslationDataModelTranslationDataDescriptorDTO'[0..1] { doc /* Source type: TranslationDataModelTranslationDataDescriptorDTO.  */ }
        ref item 'resultDataModelTranslator' : 'TranslationDataModelTranslationDataDescriptorDTO'[0..1] { doc /* Source type: TranslationDataModelTranslationDataDescriptorDTO.  */ }
        attribute 'inputDataModelRequirement' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'resultDataModelRequirement' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetInterface' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'targetInterfaceProperties' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        attribute 'operation' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'authorizationToken' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'interfaceTranslatorSettings' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
    }
    item def 'TranslationCheckTargetsRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'targetOperation' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'targets' : 'TranslationTargetDTO'[0..*] { doc /* Source type: List<TranslationTargetDTO>.  */ }
    }
    item def 'TranslationCheckTargetsResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'targets' : 'TranslationTargetDTO'[0..*] { doc /* Source type: List<TranslationTargetDTO>.  */ }
    }
    item def 'TranslationDataModelTranslationDataDescriptorDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'fromModelId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'toModelId' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'interfaceProperties' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        ref item 'configurationSettings' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
    }
    item def 'TranslationDataModelTranslatorInitializationResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'dataModelTranslatorName' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'interfaceProperties' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
    }
    item def 'TranslationDiscoveryMgmtRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'candidates' : 'ServiceInstanceResponseDTO'[0..*] { doc /* Source type: List<ServiceInstanceResponseDTO>.  */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'operation' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'interfaceTemplateNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'inputDataModelId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'outputDataModelId' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'flags' : JSONValue[0..1] { doc /* Source type: Map<String, Boolean>.  */ }
    }
    item def 'TranslationDiscoveryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'candidates' : 'ServiceInstanceResponseDTO'[0..*] { doc /* Source type: List<ServiceInstanceResponseDTO>.  */ }
        attribute 'operation' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'interfaceTemplateNames' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'inputDataModelId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'outputDataModelId' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'TranslationDiscoveryResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'candidates' : 'TranslationBridgeCandidateDTO'[0..*] { doc /* Source type: List<TranslationBridgeCandidateDTO>.  */ }
    }
    item def 'TranslationInterfaceTranslationDataDescriptorDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'fromInterfaceTemplate' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'toInterfaceTemplate' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'token' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'interfaceProperties' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
        ref item 'configurationSettings' : JSONValue[0..1] { doc /* Source type: Map<String, Object>.  */ }
    }
    item def 'TranslationNegotiationMgmtRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'targetInstanceId' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'TranslationNegotiationRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'target' : 'ServiceInstanceResponseDTO'[0..1] { doc /* Source type: ServiceInstanceResponseDTO.  */ }
        attribute 'operation' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'interfaceTemplateName' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'inputDataModelId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'outputDataModelId' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'TranslationNegotiationResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'bridgeInterface' : 'ServiceInstanceInterfaceResponseDTO'[0..1] { doc /* Source type: ServiceInstanceInterfaceResponseDTO.  */ }
        attribute 'tokenExpiresAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'tokenUsageLimit' : Integer[0..1] { doc /* Source type: Integer.  */ }
    }
    item def 'TranslationQueryListResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'TranslationQueryResponseDTO'[0..*] { doc /* Source type: List<TranslationQueryResponseDTO>.  */ }
        attribute 'count' : Integer[0..1] { doc /* Source type: long.  */ }
    }
    item def 'TranslationQueryRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'PageDTO'[0..1] { doc /* Source type: PageDTO.  */ }
        attribute 'bridgeIds' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'creators' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'statuses' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'consumers' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'providers' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'serviceDefinitions' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'interfaceTranslators' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'dataModelTranslators' : String[0..*] { doc /* Source type: List<String>.  */ }
        attribute 'creationFrom' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'creationTo' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'alivesFrom' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'alivesTo' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'minUsage' : Integer[0..1] { doc /* Source type: Integer.  */ }
        attribute 'maxUsage' : Integer[0..1] { doc /* Source type: Integer.  */ }
    }
    item def 'TranslationQueryResponseDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'status' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'usageReportCount' : Integer[0..1] { doc /* Source type: int.  */ }
        attribute 'alivesAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'message' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'provider' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'operation' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'interfaceTranslator' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'interfaceTranslatorData' : 'TranslationInterfaceTranslationDataDescriptorDTO'[0..1] { doc /* Source type: TranslationInterfaceTranslationDataDescriptorDTO.  */ }
        attribute 'inputDataModelTranslator' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'inputDataModelTranslatorData' : 'TranslationDataModelTranslationDataDescriptorDTO'[0..1] { doc /* Source type: TranslationDataModelTranslationDataDescriptorDTO.  */ }
        attribute 'outputDataModelTranslator' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'outputDataModelTranslatorData' : 'TranslationDataModelTranslationDataDescriptorDTO'[0..1] { doc /* Source type: TranslationDataModelTranslationDataDescriptorDTO.  */ }
        attribute 'createdBy' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'createdAt' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'updatedAt' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'TranslationReportRequestDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'timestamp' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'state' : String[0..1] { doc /* Source type: String.  */ }
        attribute 'errorMessage' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'TranslationTargetDTO' :> JSONMessage {
        doc /* Source: ah5-core-java-spring/documentation-tools/model.json; native Observation from matching core AMQP IDDs. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'instanceId' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'interfaces' : 'ServiceInstanceInterfaceResponseDTO'[0..*] { doc /* Source type: List<ServiceInstanceInterfaceResponseDTO>.  */ }
    }
}
