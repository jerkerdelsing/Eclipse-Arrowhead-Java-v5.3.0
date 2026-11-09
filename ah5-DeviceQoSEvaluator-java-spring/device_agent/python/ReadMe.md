# Read Me

This is a light-weight client side monitoring server that belongs to the Eclipse Arrowhead DeviceQoSEvaluator Support System.
Learn more: https://github.com/eclipse-arrowhead/ah5-device-qos-evaluator-java-spring

## Requirements

- python 3
- pip

## Install

Execute the following command from the source folder:

```
pip install -r requirements.txt
```

## Configure

Edit the `config.yaml` file to enable/disable monitoring task.

- For **network_load** monitoring the `link_address` property must be defined! This is the IP address assigned to the device when it connects to the network (via Wi-Fi or Ethernet).

## Run

Execute the `run.py` script.