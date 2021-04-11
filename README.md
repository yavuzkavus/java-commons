# Java Commons

Various utilities that can be used in Java applications. 

# Usage

#### StringUtil
```java
StringUtil.equals("a string", null); //false

StringUtil.empty(null); //true
StringUtil.empty(" \t \n"); //true

StringUtil.ifNull(null); //""
StringUtil.ifNull(null, "A default value"); //A default value

StringUtil.ifEmpty("  "); //null
StringUtil.ifEmpty(null, "A default value"); //A default value

StringUtil.fullName("Java", "Lang"); //Java Lang
StringUtil.fullName("Java", null) //Java

StringUtil.maxLength("123456789", 5); //"12345"
StringUtil.maxLength("123456789", 5, ".."); //"12345.."

StringUtil.stripHtml("<div><span>text</span> content</div>"); //text content

StringUtil.wrapSingle("1'2"); //1\'2
StringUtil.wrapDouble("1\"2"); //1\"2

StringUtil.toInt("123"); //123
StringUtil.toInt("Not an integer", 123); //123 
```
#### XMLDocument
```java
XMLDocument xml = XMLDocument.parse( new URL(url) );
//or
XMLDocument xml = XMLDocument.parse( new File(file) );
//or
XMLDocument xml = XMLDocument.parseFromText("<docs><doc>1</doc><doc>2</doc></docs>);

Element rootEl = xml.getDocumentElement();
xml.setAttributes(xml.createElement(rootEl, "doc", "3" ),
					"attr1",
					"val1"
					"att2",
					"val2");
XPath xpath = new XPath(xml.getDocument());
xpath.getString("/photo_extra_datas/data[@attr1]/@attr2"); //val2

String xmlText = xml.getXMLText();

xml.transform( new File(file) ); //save to file
```

#### LruMap
```java
LruMap<String, File> cache = new LruMap<>(cacheSize);
```

#### TimedCache
```java
//loads data from the source after a specified duration
TimedCache<List<Customer>> cache = TimedCache.<Set<String>>newBuilder()
											.supplier(CustomerDAO::loadExpired)
											.timeout(5, TimeUnit.MINUTES)
											.executor(MyClass.myExecutor)
											.build();

//Get calls not blocked during reload, previous one will be returned.
List<Customer> cache = expiredCustomers.get();
```

#### And other various utilities
...