package socs.network.node;

public class Link {

  RouterDescription router1;
  RouterDescription router2;
  short weight;
  long weightVersion;
  String weightOrigin = "";

  public Link(RouterDescription r1, RouterDescription r2) {
    router1 = r1;
    router2 = r2;
  }
}
