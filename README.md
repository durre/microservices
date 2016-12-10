# Microservice

Streamlined way of doing microservices with akka-http & rabbitmq.

## Http

Rest service based on akka http. 

## Workers

A worker is a process that listens to a specific rabbitmq queue for work to be done.

### Config

```hocon
worker {
  // The name to give worker. Used mainly in logging
  name="invoices"
  // The rabbitmq queue to look for jobs
  sourceQueue="invoice-jobs"
}
```

## Calling other microservices

```scala
class InvoiceService(client: ServiceClient)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) extends JsonFormats {

  def findInvoices(orgId: UUID, offset: Int, limit: Int, jwt: String): Future[Seq[Invoice]] = {
    val uri = s"/organizations/${orgId.toString}/invoices?offset=$offset&limit=$limit&unmatched=true"
    client.get(uri, jwt).map {
      case ClientResponse(200, ResponseBody.JsonArray(json)) => json.elements.map(_.convertTo[Invoice])
      case _ => throw new RuntimeException(s"Unable to retrieve invoices for $orgId")
    }
  }

  def updateWithMatch(invoiceId: UUID, matchedTransactionId: UUID, jwt: String): Future[Unit] = {
    val uri = s"/invoices/${invoiceId.toString}"
    val update = JsObject("matchedTransactionId" -> JsString(matchedTransactionId.toString))

    client.patch(uri, update, jwt).map {
      case ClientResponse(200, _) => Unit
      case error: ClientResponse =>
        throw new RuntimeException(s"Unable to update invoice with id: $invoiceId. Status: ${error.statusCode}")
    }
  }
}
```

