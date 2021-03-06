package it.polimi.genomics.core

import java.io._
import java.util.Base64

import it.polimi.genomics.core.DataStructures.IRVariable
import it.polimi.genomics.core.exception.GMQLDagException

/**
  * Created by Luca Nanni on 04/11/17.
  * Email: luca.nanni@mail.polimi.it
  */
object DAGSerializer {

  def serializeToBase64(dag: DAGWrapper): String = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(dag.dag.toArray)
    oos.close()
    Base64.getEncoder.encodeToString(baos.toByteArray)
  }

  def deserializeDAG(serialized: String): DAGWrapper = {
    val data = Base64.getDecoder.decode(serialized)

    try {
      val ois = new ObjectInputStream(new ByteArrayInputStream(data))
      val deserialized = ois.readObject().asInstanceOf[Array[IRVariable]]
      ois.close()
      DAGWrapper(deserialized.toList)
    } catch {
      case _: EOFException => throw new GMQLDagException("Deserialization problem")
    }
  }

}
