import java.io.{ByteArrayInputStream, File, FileOutputStream}

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.esotericsoftware.kryo.io.{Input, Output}
import com.twitter.chill.ScalaKryoInstantiator
import org.apache.commons.io.IOUtils
import org.apache.lucene.util.{BytesRef, IntsRefBuilder}
import org.apache.lucene.util.fst._

class KryoFstSerializer(accessKey: String, secretKey: String) {
  val creds = new BasicAWSCredentials(accessKey, secretKey)
  val s3 = new AmazonS3Client(creds)
  val region = Region.getRegion(Regions.EU_WEST_1)
  s3.setRegion(region)

  private val instantiator = new ScalaKryoInstantiator
  instantiator.setRegistrationRequired(false)
  private val kryo = instantiator.newKryo()

  def buildFST(map: Map[String, String]): FST[AnyRef] = {
    val outputs = new ListOfOutputs(ByteSequenceOutputs.getSingleton)
    val builder = new Builder(FST.INPUT_TYPE.BYTE1, outputs)
    val scratchInts = new IntsRefBuilder

    for (pair <- map) {
      val input = new BytesRef(pair._1)
      val output = new BytesRef(pair._2)

      builder.add(
        Util.toIntsRef(input, scratchInts),
        output
      )
    }

    val fst = builder.finish()

    fst
  }

  def serializeFSTToS3(fst: FST[AnyRef], fileName: String, bucketName: String): Unit = {
    val output = new Output(new FileOutputStream(fileName))
    kryo.writeObject(output, fst)
    output.close()
    s3.putObject(bucketName, fileName, new File(fileName))
  }

  def deserializeFSTFromS3(fileName: String, bucketName: String): Unit = {
    val s3Object = s3.getObject(
      new GetObjectRequest(
        bucketName,
        fileName
      )
    )

    val fileContentsStr = s3Object.getObjectContent
    val contentsAsBytesArray = IOUtils.toByteArray(fileContentsStr)
    val bais = new ByteArrayInputStream(contentsAsBytesArray)
    val input = new Input(bais)

    val output = kryo.readObject(input, classOf[FST[AnyRef]])
    return output
  }
}

object KryoFstSerializer {
  def main(args: Array[String]): Unit = {
    val fstBuilder = new KryoFstSerializer("", "")
    val bucketName = ""
    val fileName = "catDogFST"

    val fst = fstBuilder.buildFST(
      Map("cat" -> "5", "dog" -> "7", "dogs" -> "12")
    )

    fstBuilder.serializeFSTToS3(fst, fileName, bucketName)
    fstBuilder.deserializeFSTFromS3(fileName, bucketName)
  }
}
