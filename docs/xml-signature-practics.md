# XML数字签名实践篇

Kevin Chen - 铃盛软件Web Application Team

本文是[XML数字签名原理篇](xml-signature-introduction.md)的姐妹篇，在“原理篇”中我们从理论上探讨了XML数字签名的原理和签名的主要处理过程，以及如何验证数字签名。
胡适先生说过科学的精神就是大胆假设，谨慎求解，所以本文的目标就是从代码的层面针对“原理篇”中介绍的内容进行验证，实践是检验真理的唯一标准，尤其是编程这门实践性非常强的学科。

本文分成两部分，第一部分以一个第三方系统生成的一份XML签名文档为例，应用示例代码进行签名的验证，第二部分则通过对一份简单的原始XML文档进行签名来揭示生成XML数字签名的过程。

说明：完整的代码可以从[github](https://github.com/rng-web-geeks/back-end)下载，代码基于JDK 8+以上测试。

## 分步验证XML数字签名
我们以项目测试中使用的一份第三方权威系统（Okta）签发的XML文档为例，使用从Okta获取的可靠证书，分步骤展示如何验证这份文档的签名是否有效，最后再给出一个综合的例子。
读者可以点击链接查看或下载[XML签名文档](../src/main/resources/xml/signed-xml-example.xml)。
为了更好的从原理上验证签名的处理过程，除了DOM节点的查找和规范化处理使用了Apache的[Santuario](http://santuario.apache.org/)库，
其他步骤不借助XML数字签名的类库。

### 第一步：验证签名信息
在原理篇我们谈到，XML的数字签名是针对整个”SignedInfo"子节点规划化的内容进行签名的，为了验证签名，我们需要取得“SignInfo”整个子节点的规范化文档内容，签名方法和签名信息，
以及验证的公钥，我们一步一步来分析如何获取这些信息：

* 首先要加载被验证的XML文档
```

Document doc = XMLHelper.loadXML("xml/signed-xml-example.xml");

```

* 其次要加载验证签名的证书
```
X509Certificate verifyCert = CertificateHelper.loadCert("certs/test-certificate.pem");
```

* 有了XML文档和证书，就可以着手进行签名的验证了

```
//获取签名节点
Element signatureNode = getSignatureNode(doc.getDocumentElement());

//获取SignInfo子节点
Element signInfoNode = XMLHelper.getNextElement(signatureNode.getFirstChild());

//获取Base64解码后的签名信息
byte[] signatureValue = getSignatureValue(signatureNode);

//取得规范化处理算法
String methodUrl = getCanonicalizationMethodURI(signInfoNode);

//获取SignInfo规范化的内容
Canonicalizer canon = Canonicalizer.getInstance(methodUrl);
canon.setSecureValidation(true);
byte[] canonSignInfo = canon.canonicalizeSubtree(signInfoNode);

//创建RSA-SHA256签名实例
Signature sig = Signature.getInstance("SHA256withRSA");

//验证签名
sig.initVerify(verifyCert.getPublicKey());
sig.update(canonSignInfo);

boolean test = sig.verify(signatureValue);
System.out.println(String.format("XML data validation result is %s.", test));
``` 

如果验证通过，则说明整个"SignInfo"节点没有被篡改过，有兴趣的同学可以试着下载代码，并把原始的签名XML文档的“SignInfo”子节点改变某个属性值，
或者增加个属性看看验证的结果。
如果对规范化处理感兴趣，也可以把规范化后的“SignInfo”（new String(canonSignInfo, "UTF-8")）输出来和原始的SignInfo对比下，看看有什么不一样。

点击[VerifySignatureValue.java](../src/main/java/com/ringcentral/demo/xml/VerifySignatureValue.java)查看或下载完整的源代码.

### 第二步：验证所有Reference的引用节点摘要

在确认“SignInfo”没有被篡改的情况下，还需要进一步验证Reference对应的节点的摘要是否一致，验证过程和签名大体相似，加载XML签名文档和公钥和上面验证签名完全一致，这里就不重复了，
着重分步解析Reference的验证过程。

* 获取SignInfo所有的Reference子节点并逐一验证
```
//获取Signature节点
Element signatureNode = getSignatureNode(doc.getDocumentElement());

//从Signature节点取得SignInfo
Element signInfoNode = XMLHelper.getNextElement(signatureNode.getFirstChild());

//列出SignInfo的所有Reference子节点
Element[] references = getReferences(signInfoNode);

//循环验证所有的Reference节点对应的摘要信息
for (Element ref: references) {
    verifyReference(ref, doc);
}

```

* 验证Reference引用节点摘要
```
//查找Reference URI属性指定的引用节点
Element node = getRefNode(reference);

//计算引用节点的规范化输出XML字节数组
byte[] c14nData = getCanonicalizedXML(node, doc);

//创建“SHA-256”摘要算法实例
MessageDigest digest = MessageDigest.getInstance("SHA-256");
digest.reset();

//计算规范化XML字节摘要
byte[] calcDigest =digest.digest(c14nData);

//读取Reference的”DigestValue“子节点的Base64文本并解码成字节数组
byte[] orignalDigest = Base64.getDecoder().decode(getDigestValue(reference));

//比较原始的摘要和计算的摘要，两者一致则表明引用的节点内容没有被篡改
System.out.println(String.format("Reference %s validation result: %s", reference.getAttribute("URI"), Arrays.equals(calcDigest, orignalDigest)));

```

点击[VerifyXMLSignedInfoReference.java](../src/main/java/com/ringcentral/demo/xml/VerifyXMLSignedInfoReference.java)查看或下载完整的源代码.

## 使用XML数字签名类库验证签名

上一节分步验证XML数字签名主要是为了更方便和直观的理解签名的验证，在实际使用中，完全没必要这么做，JDK本身就提供了“javax.xml.crypto.dsig”库用于支持XML数字签名的生成和验证，使用起来略繁琐，
也可以使用Apach旗下便捷的开源的[Santuario](http://santuario.apache.org/)库，它们的内部实现和我们前面介绍的两个步骤基本一致，我们分别用刚才的例子用两种方法来验证：

Note：实际项目使用中，除了签名验证外，还需要注意防范常见的XML包装攻击，安全性这块本文就不再展开。

* 使用JDK自带的“javax.xml.crypto.dsig”库
```
Document doc = XMLHelper.loadXML("xml/signed-xml-example.xml");
X509Certificate verifyCert = CertificateHelper.loadCert("certs/test-certificate.pem");

Element signatureNode = getSignatureNode(doc.getDocumentElement());

DOMValidateContext valContext = new DOMValidateContext(certificate.getPublicKey(), signatureNode);
XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
javax.xml.crypto.dsig.XMLSignature signature = fac.unmarshalXMLSignature(valContext);

//valiate成功，说明XML签名是有效的，反之则被篡改
boolean result = signature.validate(valContext);

System.out.println(String.format("Signature validation result is: %s", result));

//当签名验证失败时，可以通过下面的代码进一步判断具体失败的原因是签名问题还是Reference的摘要验证失败
boolean sv = signature.getSignatureValue().validate(valContext);
System.out.println(String.format("Signature value validation status: %s", sv));

List refs = signature.getSignedInfo().getReferences();
for(int i=0; i<refs.size(); i++) {
  Reference ref = (Reference)refs.get(i);
  boolean refValid = ref.validate(valContext);
  System.out.println(String.format("Reference[%s] validity status is %s", i, refValid));
}
```

* 使用Apache Santuario开源库

```
Document doc = XMLHelper.loadXML("xml/signed-xml-example.xml");
X509Certificate verifyCert = CertificateHelper.loadCert("certs/test-certificate.pem");

Element signatureNode = getSignatureNode(doc.getDocumentElement());
XMLSignature signature = new XMLSignature(signatureNode, "", true);

//valiate成功，说明XML签名是有效的，反之则被篡改
boolean result = signature.checkSignatureValue(certificate.getPublicKey());

System.out.println(String.format("Signature validation result is: %s", result));

//当签名验证失败时，可以通过下面的代码进一步判断具体失败的原因是签名问题还是Reference的摘要验证失败
for(int i =0; i < signature.getSignedInfo().getLength(); i ++){
    boolean isRefValidated = signature.getSignedInfo().getVerificationResult(i);
    System.out.println(String.format("Reference[%s] validation result is %s", i, isRefValidated));
}
```

点击[VerifyXMLSignatureFullExample.java](../src/main/java/com/ringcentral/demo/xml/VerifyXMLSignatureFullExample.java)查看或下载完整的源代码.


## 生成XML数字签名


## 参考资料

[Programming With the Java XML Digital Signature API](https://www.oracle.com/technetwork/articles/javase/dig-signature-api-140772.html)

[Apache XML Security for Java](https://apache.googlesource.com/santuario-java)