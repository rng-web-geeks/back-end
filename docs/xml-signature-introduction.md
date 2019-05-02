# XML数字签名原理篇

Kevin Chen - 铃盛软件Web Application Team

说起数字签名，对安全有所涉猎的同学相信都不陌生，简单的说，数字签名是一种基于摘要算法和非对称加密技术的防止数据在传输传递过程中被篡改的一种安全技术，具体怎么做的呢？
其实现原理是对要传输的内容做个摘要，然后把摘要和用到的摘要算法使用非对称加密技术的公钥或者私钥（绝大部分情况是私钥）生成签名，这样接收方接收到数据后，把签名信息用私钥或公钥（绝大部分情况是公钥）验证来确保内容的完整性。

Note: 数字签名还有另外一个特性是不可抵赖性。

## 什么是XML数字签名
XML数字签名是在数字签名的基础上定义出来的一种数字签名规范，和普通的数字签名相比较，XML数字签名有不少优点(当然也带来了一定的复杂性)，其中一点是比较灵活，
XML数字签名既可以对传输的所有内容进行签名，也可以只对传输的一小部分内容或者几部分内容进行签名，不同的签名还可以使用不同的算法和密钥。
本文并非重点探讨XML数字签名的各种优缺点，对这部分比较感兴趣的同学可以自行Google。

## XML数字签名的应用场景
XML数字签名有着非常广泛的应用场景，可以用于一般的可靠信息交换，电子公文传输等领域。广泛使用的基于SAML2规范的跨组织间异构系统的单点登录就是在XML数字签名的基础上拓展出来的。

## XML数字签名的类型

XML数字签名规范定义了三种类型，分别是“Enveloped”， “ Enveloping”和“Detached”，这三种类型的主要差别在于XML文档结构的不同，其他方面基本上是一致的，本文重点以常用的“Enveloped”格式进行分析和探讨。

### Enveloped
Enveloped格式的XML签名，是把签名节点(Signature)嵌入在原始的XML document的里面，比如原始文档是：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<PurchaseOrder>
 <Item number="130046593231">
  <Description>Video Game</Description>
  <Price>10.29</Price>
 </Item>
 <Buyer id="8492340">
  <Name>My Name</Name>
  <Address>
   <Street>One Network Drive</Street>
   <Town>Burlington</Town>
   <State>MA</State>
   <Country>United States</Country>
   <PostalCode>01803</PostalCode>
  </Address>
 </Buyer>
</PurchaseOrder>
```

签名后生成的Signature节点作为一个子节点嵌入在“PurchaseOrder”下面：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<PurchaseOrder>
 <Item number="130046593231">
  <Description>Video Game</Description>
  <Price>10.29</Price>
 </Item>
 <Buyer id="8492340">
  <Name>My Name</Name>
  <Address>
   <Street>One Network Drive</Street>
   <Town>Burlington</Town>
   <State>MA</State>
   <Country>United States</Country>
   <PostalCode>01803</PostalCode>
  </Address>
 </Buyer>
 <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
  <SignedInfo>
   <CanonicalizationMethod
    Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/>
   <SignatureMethod
    Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
   <Reference URI="">
    <Transforms>
     <Transform
      Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
    </Transforms>
    <DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
    <DigestValue>tVicGh6V+8cHbVYFIU91o5+L3OQ=</DigestValue>
   </Reference>
  </SignedInfo>
  <SignatureValue>
   ...
  </SignatureValue>
  <KeyInfo>
   <X509Data>
    <X509SubjectName>
     CN=My Name,O=Test Certificates Inc.,C=US
    </X509SubjectName>
    <X509Certificate>
     ...
    </X509Certificate>
   </X509Data>
  </KeyInfo>
 </Signature>
</PurchaseOrder>
```

### Enveloping

“Enveloping”格式的XML签名，和“Enveloped”正好相反，它是把原始XML文档作为一个子节点，插入到新生成的“Signature”节点的“Object”子节点，原始文档和“Enveloped”相同的情况，签名后的文档类似：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
  <SignedInfo>
   <CanonicalizationMethod
    Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/>
   <SignatureMethod
    Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
   <Reference URI="#order">
    <DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
    <DigestValue>tVicGh6V+8cHbVYFIU91o5+L3OQ=</DigestValue>
   </Reference>
  </SignedInfo>
  <SignatureValue>
   ...
  </SignatureValue>
  <KeyInfo>
   <X509Data>
    <X509SubjectName>
     CN=My Name,O=Test Certificates Inc.,C=US
    </X509SubjectName>
    <X509Certificate>
     ...
    </X509Certificate>
   </X509Data>
  </KeyInfo>
  <Object ID="order">
      <PurchaseOrder>
       <Item number="130046593231">
        <Description>Video Game</Description>
        <Price>10.29</Price>
       </Item>
       <Buyer id="8492340">
        <Name>My Name</Name>
        <Address>
         <Street>One Network Drive</Street>
         <Town>Burlington</Town>
         <State>MA</State>
         <Country>United States</Country>
         <PostalCode>01803</PostalCode>
        </Address>
       </Buyer>
      </PurchaseOrder>
   </Object>
 </Signature>
 
```
```<Reference URI="#order">```
这里要特别注意"Reference"是指向Object节点，这里不能再用默认值空值了，空值代表XML文档的根节点。

### Detached
Detached格式顾名思义，就是新生成的Signature节点是作为一个额外的文档单独保存和传输，不改变原始文档，这里就不赘述了。

## XML数字签名的结构剖析
学习的一种有效方法之一就是拿一个实际的例子来进行分析，下面我们将使用我们测试中使用的一个实际例子来展示XML数字签名的结构，在看实际例子之前，
我们先从整体上熟悉下Signature的内部子结构，在这基础上进一步分析例子会有效的多。

### Signature 结构

```dtd
<Signature ID?> 
  <SignedInfo>
    <CanonicalizationMethod />
    <SignatureMethod />
   (<Reference URI? >
     (<Transforms>)?
      <DigestMethod>
      <DigestValue>
    </Reference>)+
  </SignedInfo>
  <SignatureValue> 
 (<KeyInfo>)?
 (<Object ID?>)*
</Signature>
```

* "Signature"是数字签名节点的根节点。

* “SignedInfo” 是“Signature”的第一个非空子节点，用来保存签名和摘要信息以及使用的各种算法，下面结合例子再细说。
  * “SignedInfo”的“CanonicalizationMethod”子节点用来指定生成签名的“SignedInfo”节点的规范化处理方法（规范化请参考附录的 “Exclusive XML Canonicalization”）。
  * “SignedInfo"的"SignatureMethod"子节点用来指定签名使用的摘要算法和签名算法。
  * SignedInfo"可以包含一个或多个“Reference”子节点，每个Reference用来指定某个引用的XML节点经过规范化后的摘要信息和生成摘要的方法。

* “SignatureValue”是“Signature”的第二个非空子节点，用来保存整个“SignedInfo”节点经过规范化后输出的内容的签名信息。

* “KeyInfo”是可选的，用来保存验证签名的非对称加密算法公钥（只有公钥是可以公开传播的）。

* “Object”节点也是可选的，一般只有在”Enveloping"的XML签名才会用到。

Note：对XML数字签名扩展感兴趣的可以参考[XML Signature Syntax and Processing Version 1.1](https://www.w3.org/TR/xmldsig-core/)

### Signature例子解析

```xml
<?xml version="1.0" encoding="UTF-8"?>
<saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol" ID="id143818005084022682105341102" InResponseTo="a7301b9hcg98dg82f70cjd37bfcdd"
                 IssueInstant="2019-02-20T08:19:09.255Z" Version="2.0" xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" Format="urn:oasis:names:tc:SAML:2.0:nameid-format:entity">...</saml2:Issuer>
    <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
        <ds:SignedInfo>
            <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
            <ds:SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/>
            <ds:Reference URI="#id143818005084022682105341102">
                <ds:Transforms>
                    <ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
                    <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
                        <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#" PrefixList="xs"/>
                    </ds:Transform>
                </ds:Transforms>
                <ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
                <ds:DigestValue>5xZmPQSr662MY7dP9d3zZZkToOKUCVECDjtXvsHGeYY=</ds:DigestValue>
            </ds:Reference>
        </ds:SignedInfo>
        <ds:SignatureValue>
            oq9I6/uZyhu/mfJG3SNLShLKoYEoIDTBe8YYUNOfcQqCp0Kp80baf8cnoXlDuC32fLr8R4QQ3IFwzPhmciOF4HbhgvMGMWpV2+QSMQGQf6CDwk/cwTBktDhhWRZy/hz1PeCBumCGyBEc8I89ondWlb2taO4RVUxVvbbwt42YulBFYY13T2SkyL+3i4KFNMfUhejL1M5vMZz3Zl+7uwI7QNqjBn6hIODffxno3ITvm00XUJ8dC3Gap7ew8pfpD+oxOAstzEtsjFhNijnPeemSywZhH18Fp223cnYv5I7mUoGiKW8BSArdsoPVFrnMy/39EGRNV8bi3Qz70x7t/NxF7A==
        </ds:SignatureValue>
        <ds:KeyInfo>
            <ds:X509Data>
                <ds:X509Certificate>...
                </ds:X509Certificate>
            </ds:X509Data>
        </ds:KeyInfo>
    </ds:Signature>
    <saml2p:Status xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol">
        <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
    </saml2p:Status>
    <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" ID="id14381800508573432508875164"
                     IssueInstant="2019-02-20T08:19:09.255Z" Version="2.0" xmlns:xs="http://www.w3.org/2001/XMLSchema">
        ...
    </saml2:Assertion>
</saml2p:Response>
```

这是我们测试中使用的一个实际例子，为了节省篇幅把和主题无关的内容节点删除。
首先，我们来分析下“Signature”的第一个子节点“SignedInfo”，XML数字签名是对整个“SingedInfo"节点规范化后的内容进行签名，这点非常重要。

* “SignedInfo”是“Signature”的第一个非空子节点，它包含“CanonicalizationMethod”，“SignatureMethod”，一个或多个”Reference“子节点。
  * "SignatureInfo"的子节点“CanonicalizationMethod”
的“Algorithm”属性值是“http://www.w3.org/2001/04/xmldsig-more#rsa-sha256” ，表示“SingedInfo”节点签名前进行规范化处理的算法，为什么要进行规范化处理在下节“XML数字签名的处理过程”
会进一步说明。

  * “SignatureInfo”的子节点“SignatureMethod”的“Algorithm”值是"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256" 表示数字签名的方法是“SHA256-RSA”，即先对"SignedInfo"节点规范化后的输出使用“SHA256”摘要算法计算摘要，然后用“RSA”算法进行签名，对摘要算法和非对称加密签名算法不熟悉的同学请自行Google，
本文不再展开。

  * “SignedInfo"只有一个子节点”Reference“，它的URL属性等于”#id143818005084022682105341102“，说明”Reference“节点的摘要是针对Id为”id143818005084022682105341102“的
DOM节点计算的，这个节点就是“Response"根节点。
URI属性是可选的，如果为空，则表明引用的节点是XML文档的根节点。

  * “Reference”的子节点“Transforms”用于说明对“Response”节点进行摘要计算时，需要先进行规范化处理，它有两个子节点，第一个子节点的Algorithm属性值是“http://www.w3.org/2000/09/xmldsig#enveloped-signature”
  ，说明这是一个“Enveloped"的XML数字签名，所以Response节点包含了”Signature"签名节点，计算摘要前要先把“Response”的子节点“Signature”先剔除掉。
  “Transforms”子节点也是可选的，如果为空则直接计算摘要。规范定义了强制性的规范化处理和摘要算法。

    * “Transforms”的第二个子节点Algorithm属性值是”http://www.w3.org/2001/10/xml-exc-c14n#“ ，眼尖的同学可能会发现这个值和前面”CanonicalizationMethod“节点的属性值是一样的，
这个属性说明“Response”节点剔除“Signature"节点后，需要进行算法为”xml-exc-c14n“的规范化操作。

    * “Reference”接下来的子节点是“DigestMethod”，它的Algorithm属性值是“http://www.w3.org/2001/04/xmlenc#sha256” ，表示“Response”节点进过规范化处理后，使用“SHA256"摘要
算法计算摘要。

    * “Reference"最后一个子节点是”DigestValue“，这个节点用来存储Response经过规范化处理后的摘要值。

* ”SignatureValue“是”Signature“的第二个非空子节点，它的内容是整个”SignedInfo“节点经过规范化后的内容的数字签名, 并使用Base64编码算法转换成可见的字符串。
```
oq9I6/uZyhu/mfJG3SNLShLKoYEoIDTBe8YYUNOfcQqCp0Kp80baf8cnoXlDuC32fLr8R4QQ3IFwzPhmciOF4HbhgvMGMWpV2+QSMQGQf6CDwk/cwTBktDhhWRZy/hz1PeCBumCGyBEc8I89ondWlb2taO4RVUxVvbbwt42YulBFYY13T2SkyL+3i4KFNMfUhejL1M5vMZz3Zl+7uwI7QNqjBn6hIODffxno3ITvm00XUJ8dC3Gap7ew8pfpD+oxOAstzEtsjFhNijnPeemSywZhH18Fp223cnYv5I7mUoGiKW8BSArdsoPVFrnMy/39EGRNV8bi3Qz70x7t/NxF7A==
```

* “KeyInfo” 是可选节点，这里存储的是用于验证“SignatureValue”签名的公钥，注意对收到的签名进行验证时不能简单的使用这个公钥，而应该使用从正式途径获取的受信任的公钥来验证，
不然任何人就可以拦截内容，篡改后用自己的私钥进行签名并包含自己的公钥在XML文件。“Signature”包含验证公钥的目的之一是签名方可能有多个密钥对，签名的时候选择其中一个密钥对，这样接收信息的
一方肯定需要知道收到的数据的签名需要使用哪个密钥对的公钥进行验证。一般做法是把“KeyInfo”存储的公钥和接收方从正式途径获取的信任公钥（可能有多个）进行比对，如果和其中的某个公钥一致，则为受信任的
公钥，如果匹配失败，则表明收到的信息不可靠。

## XML数字签名的处理过程

### 签名过程
XML的数字签名主要工作是根据要签名的内容创建“Signature“节点，这包含三个必要步骤，首先生成”Reference“节点，然后再这个基础上创建”SignedInfo“，最后针对”SignedInfo“生成数字签名，并最终
生成“Signature”节点，下面以“Enveloped”格式为例介绍这几个必要步骤：

* 生成"Reference"节点。
  首先确定受保护的XML节点，可以是整个文档的根节点或者某（几）个子节点，确定完受保护的XML节点后就可以创建“Reference”根节点，如“<ds:Reference URI="#id143818005084022682105341102">”。
  
  第二步需要确定受保护的节点在计算摘要前需要进行什么转换，主要是需要进行规范化处理。为什么生成摘要前要进行规范化呢？这主要是为了规范和提升跨组织和跨系统之间的互操作性。
  对XML有了解的同学都知道，同样语义的XML文档可以有不同的表现方式，举个栗子：
  ```xml
    <Book id="001" name="Javascript core book"></Book>
  ```
  ```xml
    <Book name="Javascript core book"   id="001"/>
   ```
   上面这两个XML节点仔细观察我们可以发现他们的不同之处，前面的的节点有闭合标签，后面的节点是自闭合标签，而且两个标签的属性虽然一样，但顺序是不同的，属性之间的空白间隔也
   不一样，但这两个标签在语义上是完全等价的。
   
   由于XML文档的这些特点，对于语义完全一致的XML文档，经过不同XML工具的序列化和反序列化后可能是完全不同的，据此计算的摘要信息也可能是完全不同的，如果不进行规范化处理，
   就给不同系统不同工具的互操作性带来一定的困难和挑战，因此在生成摘要信息前要进行规范化处理，并把使用的规范化处理算法打包到“Transforms“节点中一起发送给接收方，通常的规范化处理方法是
   “http://www.w3.org/2001/10/xml-exc-c14n#”， 有兴趣的同学可以参考[Exclusive XML Canonicalization](https://www.w3.org/TR/xml-exc-c14n/)， 本文不再展开介绍规范化的内容。
  
  最后一步是把经过规范化处理后的XML文档内容，按照确定好的摘要算法计算摘要，常用的有MD5 （已经不安全，不建议使用）， SHA1(2017年被Google证实可以发生碰撞，也不安去了)，SHA3, SHA256等，然后生成“DigestMethod”和“DigestValue”子节点。
  
  把上面几个步骤串起来，生成后的Reference节点大概是这样：
  ```xml
    <ds:Reference URI="#id143818005084022682105341102">
      <ds:Transforms>
        <ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
        <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#">
          <ec:InclusiveNamespaces xmlns:ec="http://www.w3.org/2001/10/xml-exc-c14n#" PrefixList="xs"/>
        </ds:Transform>
      </ds:Transforms>
      <ds:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
      <ds:DigestValue>5xZmPQSr662MY7dP9d3zZZkToOKUCVECDjtXvsHGeYY=</ds:DigestValue>
     </ds:Reference>
  ```
  如果有多个节点（非根节点的情况）需要进行签名，可以重复上面的步骤为每个需要签名的节点生成一个对应的"Reference"节点。
  
* 生成“SignedInfo”节点，
  Reference节点准备完毕后，就可以准备生成“SignedInfo”节点了，创建“SignedInfo”节点前，和生成摘要信息一样，需要先确定“SignedInfo”节点的规范化处理算法，和数字签名的算法，并通过
  “CanonicalizationMethod”和“SignatureMethod”节点来保存：
  
  ```xml
        <ds:SignedInfo>
            <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
            <ds:SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/>
            <ds:Reference URI="#id143818005084022682105341102">
                ...
            </ds:Reference>
        </ds:SignedInfo>
  ```
  
* 生成“SignatureValue”节点，
创建“Signature”节点的最后一个必要步骤就是把第二步生成的“SignedInfo"节点，按照选定的规范化算法进行处理，并使用确定好的签名算法生成签名信息，然后保存到”SignatureValue“节点。

* 生成“Signature”节点，
“SignedInfo"和”SignatureValue“节点准备好之后，就可以创建"Signature"节点了，这一步比较简单，只需要把”SignedInfo“和”SignatureValue“作为”Signature“的子节点就可以，
如果需要，可以把生成“SignatureValue”的签名私钥对应的公钥放到“KeyInfo”子节点，这个子节点是可选的，最终生成的“Signature”节点可以参考 [Signature例子解析]中的例子。
因为是Enveloped格式的数字签名，“Signature”节点创建完，直接作为"Reference“ URI指向的节点的子节点就可以了，
这个例子是以”Signature“作为ID为”id143818005084022682105341102“（也就是Response节点）的节点的子节点。

### 签名验证过程

了解了XML的签名过程，验证签名就简单了，反过来做就行，分两步：

* 验证“SignatureValue”
  收到信息的接收方，按照“SignedInfo”子节点”CanonicalizationMethod“指定的规范化算法处理整个”SignedInfo“节点，注意是整个"SignedInfo”节点，然后按照“SignatureMethod”
  指定的数字签名算法验证收到的签名信息，这一步如果验证通过，可以确保整个“SignedInfo”节点的内容没有被篡改。
  
* 验证所有“Reference”，
  验证完“SignatureValue”后，还需要逐一验证”SignedInfo"节点包含的“Reference",验证过程如下：
  1. 通过“Reference”节点的“URI”属性找到对应XML DOM节点(通常还需要验证这个ID在整个XML文档具有唯一性来避免XML的包装攻击)。
  2. 把找到的DOM节点按照"Reference"的”Transforms“指定的处理方法进行规范化处理。
  3. 按照“DigestMethod”指定的摘要算法对第二步规范化处理后的内容计算摘要。
  4. 把上面一步计算的摘要信息和“DigestValue”节点的摘要信息（需要进行Base64解码）进行比较，如果一致则通过，反之检验失败。

签名的验证通过这两步就可以确保传输的内容没有经过篡改，如果有人篡改了内容，则Reference的摘要验证会失败，如果篡改人重新生成篡改后的文档的摘要信息，则“SignedInfo”的签名验证会失败，
除非篡改人拥有私钥(比如私钥泄露的情况)。

## 参考资料

[Programming With the Java XML Digital Signature API](https://www.oracle.com/technetwork/articles/javase/dig-signature-api-140772.html)

[https://zh.wikipedia.org/wiki/XML_Signature](https://zh.wikipedia.org/wiki/XML_Signature)

[XML Signature Syntax and Processing Version 1.1](https://www.w3.org/TR/xmldsig-core/)

[Exclusive XML Canonicalization](https://www.w3.org/TR/xml-exc-c14n/)