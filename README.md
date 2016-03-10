Big Data Europe EU project (BDE)
==============
BDE Event Detection module

>       BDEBase
>       BDETwitterListener
>       BDERSSCrawler
>       BDELocationExtraction
>       BDECLustering

> **twitter listener:**

- The twitter listener requires an active twitter developer account (dev.twitter.com), and the four 
credentials generated in res/twitter.properties to function
- Functional with MySQL / Cassandra backend. Must be pre-configured and waiting for connections. Check res/db dir
- Can monitor rest/stream API: check res/twitter.properties file

> **RSS Crawler:**
- The RSS crawler requires a file containing the URLs (MUST be RSS feeds) to crawl. 
- Functional with MySQL / Cassandra backend. Must be pre-configured and waiting for connections. Check res/db dirs

> **Location Extraction:**
- The module uses OpenNLP publicly available trained models for location entity extraction from news/twitter and for each one, delegates a call to the uoa API to retrieve the bounding box metadata and save them to the backend. 
- Functional with Cassandra backend. Must be pre-configured and waiting for connections. Check res/db dir

> **Clustering:**
- The module is implemented on Spark (Java 7 interface), in a combined Java/Scala src. Utilizes the newsum custom clustering algorithm, and the MCL algorithm. It processes batches of articles/items, not streamed data.

*author:* George K. <gkiom@iit.demokritos.gr>

