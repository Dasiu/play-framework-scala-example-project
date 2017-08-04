package commons.repositories

import commons.models.{Descending, IdMetaModel, Ordering, Property}
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api.{DBIO => _, MappedTo => _, Rep => _, TableQuery => _, _}
import slick.lifted._

import scala.concurrent.ExecutionContext.Implicits._

trait BaseRepo[ModelId <: BaseId[Long], Model <: WithId[Long, ModelId], ModelTable <: IdTable[ModelId, Model]] {

  protected val mappingConstructor: Tag => ModelTable
  protected val metaModelToColumnsMapping: Map[Property[_], (ModelTable) => Rep[_]]
  implicit protected val modelIdMapping: BaseColumnType[ModelId]

  // lazy required to init table query with concrete mappingConstructor value
  lazy val query: TableQuery[ModelTable] = TableQuery[ModelTable](mappingConstructor)

  protected val metaModel: IdMetaModel

  def all: DBIO[Seq[Model]] = all(List(Ordering(metaModel.id, Descending)))

  // todo streaming in batches?
  def all(orderings: List[Ordering]): DBIO[Seq[Model]] = {
    if (orderings == null) all
    else orderings match {
      case Nil => all
      case _ =>
        // multiple sortBy calls are reversed comparing to SQLs order by clause
        val slickOrderings = orderings.map(toSlickOrderingSupplier).reverse

        var sortQuery = query.sortBy(slickOrderings.head)
        slickOrderings.tail.foreach(getSlickOrdering => {
          sortQuery = sortQuery.sortBy(getSlickOrdering)
        })

        sortQuery.result
    }
  }

  private def toSlickOrderingSupplier(ordering: Ordering): (ModelTable) => ColumnOrdered[_] = {
    implicit val Ordering(property, direction) = ordering
    val getColumn = metaModelToColumnsMapping(ordering.property)
    getColumn.andThen(RepoHelper.createSlickColumnOrdered)
  }

  def byId(modelId: ModelId): DBIO[Option[Model]] =
    if (modelId == null) DBIO.failed(new NullPointerException)
    else query.filter(_.id === modelId).result.headOption

  def create(model: Model): DBIO[Model] =
    if (model == null) DBIO.failed(new NullPointerException)
    else query.returning(query.map(_.id)).+=(model)
      .flatMap(id => byId(id))
      .map(_.get)

  def update(model: Model): DBIO[Model] =
    if (model == null) DBIO.failed(new NullPointerException)
    else query
      .filter(_.id === model.id)
      .update(model)
      .flatMap(_ => byId(model.id)) // todo what about transactions?
      .map(_.get)

}

abstract class IdTable[Id <: BaseId[Long], Entity <: WithId[Long, Id]]
(tag: Tag, schemaName: Option[String], tableName: String)
(implicit val mapping: BaseColumnType[Id])
  extends Table[Entity](tag, schemaName, tableName) {

  def this(tag: Tag, tableName: String)(implicit mapping: BaseColumnType[Id]) = this(tag, None, tableName)

  protected val idColumnName: String = "id"

  final def id: Rep[Id] = column[Id](idColumnName, O.PrimaryKey, O.AutoInc)
}