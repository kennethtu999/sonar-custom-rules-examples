This example demonstrates how to write **Custom Rules** for the SonarQube Java Analyzer (aka SonarJava).

It requires to install **SonarJava** **4.7.1.9272** on your SonarQube 5.6+

## History
### 20171121
-修改原有的規則 **Members of Spring components should be injected**，修改當@Scope='prototype'時，不要進行檢查，因為在此範圍定義時，變數並不會有Thread Safe須要處理的情況
