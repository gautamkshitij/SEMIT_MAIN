#!/usr/bin/perl

foreach $in (@ARGV) {
  $out = "temp/javasrc/$in";
  print "$in -> $out\n";

  open(IN, "<$in") || die;
  $text = "";
  while (<IN>) { $text .= $_ }
  close IN;

  $_ = $text;

  $_ = escapeComments($_);
  while (s!(<[^>]*?)/\*.*?\*/([^<]*?>)!$1$2!gs) {} # delete comments within type params

  while (1) {
    while (/\b(?:class|interface)\s+\w+\s*<([a-zA-Z_\$0-9., \t\n\r]*)>/gs) {
      for $m (split(/,/, $1)) {
	if ($m =~ /(\w+)\s+(?:extends|implements)\s+(.*)/) {
	  ($name,$basic) = ($1,$2);
	} else {
	  ($name,$basic) = ($m,"java.lang.Object");
	}
	if ($name ne $basic) {
	  $occ = s/\b$name\b/$basic/g;
	  print "$name -> $basic ($occ occurrences)\n";
	}
      }
    }

    last unless s/<[a-zA-Z_\$0-9., \t\n\r]*>//gs;
  }
  $_ = unescapeComments($_);

  open(OUT, ">$out") || die;
  print OUT $_;
  close OUT;
}

sub escape {
  local $_ = shift;
  s/(.)/$1$1#/gs;
  return $_;
}

sub unescape {
  local $_ = shift;
  s/(.)../$1/gs;
  return $_;
}

sub escapeComments {
  local $_ = shift;
  s!/\*(.*?)\*/!"/*".escape($1)."*/"!egs;
  return $_;
}

sub unescapeComments {
  local $_ = shift;
  s!/\*(.*?)\*/!"/*".unescape($1)."*/"!egs;
  return $_;
}
