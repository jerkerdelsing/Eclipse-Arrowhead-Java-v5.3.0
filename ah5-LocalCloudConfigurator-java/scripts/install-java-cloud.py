from pathlib import Path
import argparse,shutil,yaml
p=argparse.ArgumentParser();p.add_argument('--cloud',type=Path,required=True);p.add_argument('--private-dir',type=Path,required=True);args=p.parse_args();file=args.cloud/'compose.yaml';s=file.read_text();backup=args.cloud/'compose.pre-java-localcloudconfigurator-20261030.yaml'
if not backup.exists():shutil.copy2(file,backup)
if '\n  localcloudconfigurator:\n'not in s:
 service='''
  # Operations tool: Java source is primary Eclipse-Arrowhead-java, executor uses catalogued host actions.
  localcloudconfigurator:
    image: eclipse-arrowhead/localcloudconfigurator-java:5.4.0
    build:
      context: ../../Eclipse-Arrowhead-java/ah5-LocalCloudConfigurator-java
    restart: unless-stopped
    read_only: true
    cap_drop: [ALL]
    security_opt: ["no-new-privileges:true"]
    mem_limit: 256m
    pids_limit: 128
    tmpfs: [/tmp]
    secrets:
      - { source: JAVA_LCC_API_TOKEN, target: java-lcc-api-token }
      - { source: JAVA_LCC_API_KEY, target: java-lcc-api-key }
    environment:
      COLONIES_SERVER_HOST: colonies-server
      COLONIES_SERVER_PORT: "50080"
      COLONIES_TLS: "false"
      COLONIES_COLONY_NAME: arrowhead-java-localcloud
      COLONIES_PRVKEY_FILE: /run/secrets/java-lcc-api-key
      LCC_API_TOKEN_FILE: /run/secrets/java-lcc-api-token
    volumes: [java-lcc-designs:/state]
    networks: [java-lcc-console, java-lcc-colony]
    ports: ["127.0.0.1:8097:8097"]
'''
 s=s.replace('\nsecrets:\n',service+'\nsecrets:\n',1)
 s=s.replace('\nsecrets:\n','\nsecrets:\n  JAVA_LCC_API_TOKEN: { file: '+str(args.private_dir/'api-token')+' }\n  JAVA_LCC_API_KEY: { file: '+str(args.private_dir/'api-key')+' }\n',1)
 s=s.replace('\nnetworks:\n','\nnetworks:\n  java-lcc-console:\n    internal: true\n    ipam:\n      config: [{subnet: "10.240.92.0/28"}]\n  java-lcc-colony:\n    external: true\n    name: sos-colonyos_default\n',1)
 s=s.replace('\nvolumes:\n','\nvolumes:\n  java-lcc-designs:\n',1)
 start=s.index('\n  cloud-monitor-api:\n');end=s.index('\n  docker-proxy:\n',start);block=s[start:end];block=block.replace('    environment:\n','    environment:\n      LOCAL_CLOUD_CONFIGURATOR_URL: http://localcloudconfigurator:8097\n      LOCAL_CLOUD_CONFIGURATOR_TOKEN_FILE: /run/secrets/java-lcc-api-token\n',1).replace('    secrets:\n','    secrets:\n      - { source: JAVA_LCC_API_TOKEN, target: java-lcc-api-token }\n',1).replace('networks: [arrowhead, console-docker, console]','networks: [arrowhead, console-docker, console, java-lcc-console]');s=s[:start]+block+s[end:]
# Future manager builds must use the primary source, even though cloud deployment files stay here.
s=s.replace('context: ../ah5-support-java-spring/LocalCloudManager/api','context: ../../Eclipse-Arrowhead-java/ah5-LocalCloudManager-java-spring/api').replace('context: ../ah5-support-java-spring/LocalCloudManager/ui','context: ../../Eclipse-Arrowhead-java/ah5-LocalCloudManager-java-spring/ui').replace('context: ../ah5-support-java-spring/LocalCloudManager/docker-proxy','context: ../../Eclipse-Arrowhead-java/ah5-LocalCloudManager-java-spring/docker-proxy')
d=yaml.safe_load(s);assert d['services']['localcloudconfigurator']['networks']==['java-lcc-console','java-lcc-colony'];file.write_text(s);print('Java cloud deployment updated; manager/configurator sources point to primary Java tree')
