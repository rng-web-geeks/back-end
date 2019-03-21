# XML数字签名原理篇

说起数字签名，对安全有所涉猎的同学相信都不陌生，简单的说，数字签名是一种基于摘要算法和非对称加密技术的防止数据在传输传递过程中被篡改的一种安全技术，具体怎么做的的呢？其实现原理是对要传输的内容做个摘要，然后把摘要和用到的摘要算法使用非对称加密技术的公钥或者私钥进行加密，这样接收方接收到数据后，把加密的摘要信息用私钥或公钥解开，用相同的摘要算法计算收到的内容摘要进行比较来确保内容的完整性。

##什么是XML数字签名
XML数字签名是在数字前面的基础上定义出来的一种数字签名规范，和普通的数字签名相比较，XML数字签名有不少优点(当然也带来了一定的复杂性)，首先是比较灵活，XML数字签名既可以对传输的所有内容进行签名，也可以只对传输的一小部分内容或者几部分内容进行签名，不同的签名还使用不同算法。
本文并非重点探讨XML数字签名的各种优缺点，对这部分比较感兴趣的同学可以自动Google。

##XML数字签名的类型

XML数字签名规范定义了三种类型，分别是“Enveloped”， “ Enveloping”和“Detached”，这三种类型的主要差别在于XML文档结构的不同，其他方面基本上是一致的，本文重点以常用的“Enveloped”格式进行分析和探讨。

### Enveloped
Enveloped格式的XML签名，是把签名节点(Signature)潜在XML document的里面，比如原始文档是：

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

“Enveloping”格式的XML签名，和“Enveloped”正好相反，它是把原始XML文档作为一个子节点，插入到新生成的“Signature”节点的的“Object”子节点，原始文档和“Enveloped”相同的情况，签名后的文档类似：
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
  <Object>
      <PurchaseOrder ID="order">
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
这里要特别注意"Reference"是指向Object的子节点“Purchaseorder”，这里不能在用默认值空值了，空值XML文档的根节点。

### Detached
Deached格式顾名思义，就是新生成的Signature节点是作为一个额外文档单独保存和传输，并改变原始文档，这里就不赘述了。

##XML数字签名的结构

TBD

##XML数字签名的处理过程

TBD

## 参考资料

[Programming With the Java XML Digital Signature API](https://www.oracle.com/technetwork/articles/javase/dig-signature-api-140772.html)

[https://zh.wikipedia.org/wiki/XML_Signature](https://zh.wikipedia.org/wiki/XML_Signature)

[XML Signature Syntax and Processing Version 1.1](https://www.w3.org/TR/xmldsig-core/)

[Exclusive XML Canonicalization](https://www.w3.org/TR/xml-exc-c14n/)