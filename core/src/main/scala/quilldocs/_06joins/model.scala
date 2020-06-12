package quilldocs._06joins

object model {

  case class Person(id: Int, name: String)
  case class Address(street: String, zip: Int, fk: Int)
  case class Company(zip: Int)
  case class Employer(id: Int, personId: Int, name: String)
  case class Contact(personId: Int, phone: String)
}
