# imcd21Project



## Installing
The computer should fulfill the following requirements:
* Java (RE or DK) environment v8 must be installed (or, alternatively, OpenJDK 8). Java 8 can be downloaded from: https://www.java.com/en/download/
* At least 4 GB of RAM are recommended.

## Code
The source code is available in this repository inside the ['src'](https://github.com/CrisesUrv/microaggregation-based_anonymization_tool/tree/master/data_anonymization_tool/src/cat/urv) folder. The code is divided into five packages:
* *anonymization*: includes the entity and control classes and the implementation of the anonymization algorithms
* *exception*: includes the exception classes
* *test*: includes a class with examples on how to use the API calls
* *main*: includes the runnable main class
* *utils*: includes different support classes implementing ontology access functions, distance calculators, comparators, a xml reader and a file access manager

The following figure shows the UML class diagram of the main classes
<img src="img/anonymization.jpg" width="800" />

The source code can be imported to Java IDEs (e.g. Eclipse) by cloning or downloading the project from the [main page](https://github.com/CrisesUrv/microaggregation-based_anonymization_tool) on github.

## Running
To run, access the folder where the mAnt.jar file has been stored and execute the following command from the console:
```
java -jar -Xmx1024m -Xms1024m mAnt.jar dataset_name configuration_file_name
```
where the 'dataset_name' corresponds to the name of the CSV dataset to be anonymized and the 'configuration_file_name' corresponds to the XML file specifying the configuration parameters for the dataset.

The -Xmx and -Xms parameters specify the amount of memory that will be available for the application. These can be modified according to the size of the dataset and the amount of RAM available in the system.

The resulting anonymized dataset will be stored in the same directory, with the same name as the original dataset but with '\_anom' suffix. In addition, several metrics stating the information loss resulting from the anonymization are provided.

Ejemplo de tabla:

| Attribute name  | data type        |
| --------------- | ---------------- |
| Patient_ID      | categoric        |
| Name            | categoric        |
| Last1           | categoric        |
| Last2           | categoric        |
| Gender          | categoric        |
| Age             | numeric_discrete |
| ZipCode         | categoric        |
| Episode_ID      | categoric        |
| Diagnosis_IDini | semantic         |
| Admission_date  | date             |
| Discharge_date  | date             |
| Diagnosis_ID    | semantic         |

Semantic attributes (Diagnosis_IDini and Diagosis_ID) are expressed with SNOMED-CT codes. To semantically manage them [[1][2][3]](#Resources), an OWL ontology modeling the domain of this values is needed. This ontology (snomed-ontology.owl) can be generated from the [SNOMED-CT International Edition](https://www.nlm.nih.gov/healthit/snomedct/international.html) (RF2 format) files with the ['Snomed OWL Toolkit'](https://github.com/IHTSDO/snomed-owl-toolkit) tool as follows:

```
java -jar snomed-owl-toolkit.jar -rf2-to-owl -rf2-snapshot-archives SnomedCT_InternationalRF2.zip
```   

where 'SnomedCT_InternationalRF2.zip' corresponds to the file name of the RF2 SNOMED-CT release.
For copyright reasons, the 'snomed-ontology.owl' file is not included in this project.

Two XML configuration files are included to characterize the dataset and its protection: "properties1Snomed.xml", which is configured to use *3*-anonymity on quasi-identifiers and "properties2Snomed.xml", which is configured to use *3*-anonymity on quasi-identifiers and *0.25*-closeness on confidential attributes.

For example, to perform the anonymization with "properties1Snomed.xml", execute the follow command in the console:

```
java -jar -Xmx1024m -Xms1024m ./mAnt.jar ./data_example_snomed.txt ./properties1Snomed.xml
```
As result, an anonymized dataset named "dataset_example_anom.txt" is generated in the same directory.

The second dataset available in the folder corresponds to the [UCI's Adult dataset](https://archive.ics.uci.edu/ml/datasets/Adult). It countains 30,162 complete records of census income information. The attributes it contains are the following:

| Attribute name | data type        |
| -------------- | ---------------- |
| age            | numeric_discrete |
| workclass      | semantic         |
| fnlwgt         | numeric_discrete |
| education      | semantic         |
| education-num  | numeric_discrete |
| marital-status | semantic         |
| occupation     | semantic         |
| relationship   | semantic         |
| race           | semantic         |
| sex            | semantic         |
| capital-gain   | numeric_discrete |
| capital-loss   | numeric_discrete |
| hours-per-week | numeric_discrete |
| native-country | semantic         |
| prediction     | categoric        |
