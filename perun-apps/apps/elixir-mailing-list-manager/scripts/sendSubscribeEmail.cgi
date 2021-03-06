#!/usr/bin/perl

use CGI qw(:standard);
use strict;
use warnings;
use Crypt::OpenSSL::AES;
use Crypt::CBC;
use MIME::Base64::URLSafe;
use JSON;

my $q = CGI->new;
print $q->header(-type=>'application/json', -charset=>'UTF-8');

my ($email, $mailingList, $timestamp, $callback);

my $emailRegularExp = '^[-_A-Za-z0-9+]+(\.[-_A-Za-z0-9]+)*@[-A-Za-z0-9]+(\.[-A-Za-z0-9]+)*(\.[A-Za-z]{2,})$';

#get MailingList from param
$email = param("email");
$callback = param("callback");
$mailingList = 'info@elixir-czech.cz';
my $firstname = param("name");
my $lastname = param("surname");

#reaction to not defined callback
if(!defined($callback)) {
  my $errorId = "1";
  my $message = "Callback is missing.!";
  my $name = "CallbackNotDefinedException";
  my %exception_hash = ('errorId'=>$errorId, 'message'=>$message, 'name'=>$name);
  my $json = encode_json \%exception_hash;
  print "null($json);";
  exit 0;
}

#reaction to not defined parameters
if(!defined($email)) {
  my $errorId = "2";
  my $message = "Email is not defined!";
  my $name = "EmailNotDefinedException";
  my %exception_hash = ('errorId'=>$errorId, 'message'=>$message, 'name'=>$name);
  my $json = encode_json \%exception_hash;
  print "$callback($json);";
  exit 0;
}

#regex test
if($email !~ /$emailRegularExp/) {
  my $errorId = "3";
  my $message = "Email is not in correct format!";
  my $name = "EmailNotInCorrectFormatException";
  my %exception_hash = ('errorId'=>$errorId, 'message'=>$message, 'name'=>$name);
  my $json = encode_json \%exception_hash;
  print "$callback($json);";
  exit 0;
}

#if firstname or last name not exists in get parameter
if(!defined($firstname) || !defined($lastname)) {
  my $errorId = "4";
  my $message = "Firstname or lastname missing.!";
  my $name = "NameMissingException";
  my %exception_hash = ('errorId'=>$errorId, 'message'=>$message, 'name'=>$name);
  my $json = encode_json \%exception_hash;
  print "$callback($json);";
  exit 0;
}

#check if first name and last name contains only alphabet chracters
if($firstname !~ /^[a-zA-Z ]*$/ || $lastname !~ /^[a-zA-Z ]*$/ ) {
  my $errorId = "5";
  my $message = "Wrong format of first or last name";
  my $name = "NameFromatException";
  my %exception_hash = ('errorId'=>$errorId, 'message'=>$message, 'name'=>$name);
  my $json = encode_json \%exception_hash;
  print "$callback($json);";
  exit 0;
}

#remove trailing a leading whitespace
$firstname =~ s/^\s*(.*?)\s*$/$1/;
$lastname =~ s/^\s*(.*?)\s*$/$1/;


#get timestamp
$timestamp = time;

#get aes encrypted and base64 encoded text
my $key = "PerunRulezz";
my $cipher = Crypt::CBC->new(
              -key       => $key,
              -keylength => '256',
              -cipher    => "Crypt::OpenSSL::AES"
);
my $plainText = $firstname . "|" . $lastname . "|" .  $email . "|" . $mailingList . "|" . $timestamp;
my $encryptedText;

eval {$encryptedText = $cipher->encrypt($plainText);};
if ($@) {
  my $errorId = "4";
  my $message = "Problem with encrypting data: $@";
  my $name = "EncryptingFailedException";
  my %exception_hash = ('errorId'=>$errorId, 'message'=>$message, 'name'=>$name);
  my $json = encode_json \%exception_hash;
  print "$callback($json);";
  exit 0;
}
my $encodedEncryptedText;
eval {$encodedEncryptedText = urlsafe_b64encode($encryptedText);};
if ($@) {
  my $errorId = "5";
  my $message = "Problem with encoding data: $@";
  my $name = "EncodingFailedException";
  my %exception_hash = ('errorId'=>$errorId, 'message'=>$message, 'name'=>$name);
  my $json = encode_json \%exception_hash;
  print "$callback($json);";
  exit 0;
}

#send email with informations
my $urlAddress = "https://perun.metacentrum.cz/apps-non/elixir-mailing-list-manager/subscribe-confirmation.html?callback=neco&secret=" . $encodedEncryptedText;
open(MAIL, "|/usr/sbin/sendmail -t");
#Email header
my $from = 'perun@cesnet.cz';
my $replyTo = 'support@elixir-czech.cz';
print MAIL "To: $email\n";
print MAIL "Reply-To: $replyTo\n";
print MAIL "From: $from\n";
print MAIL "Subject: Subscribe to $mailingList\n\n";
#Email Body
print MAIL "Hello, \n\n";
print MAIL "we got a request for subscribe your e-mail address $email to mailing list $mailingList.\n\n";
print MAIL "If you wish to be subscribed to $mailingList please click on following confirmation link.\n\n";
print MAIL "$urlAddress\n\n";
print MAIL "If you don't want to subscribe or you got this e-mail by mistake you can ignore it.\n";
print MAIL "In case of any troubles contact us on $replyTo";
#Send Mail
if(!close(MAIL)) {
  my $errorId = "6";
  my $message = "Sendmail was not able to send email!";
  my $name = "EmailNotSendException";
  my %exception_hash = ('errorId'=>$errorId, 'message'=>$message, 'name'=>$name);
  my $json = encode_json \%exception_hash;
  print "null($json);";
  exit 0; 
}

#Return 
print "$callback({});";
