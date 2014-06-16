<?php
class DB {

	private static $instance = NULL;

	private function __construct() { }
	private function __clone() { }

	public static function connect() {
		if (getenv('prod') == "YES") {
			$dbHost = getenv('DB_HOST');
			$dbName = getenv('DB_NAME');
			$dbUser = getenv('DB_USER');
			$dbPass = getenv('DB_PASS');
		}
		else {
			$config = json_decode(file_get_contents('lib/local.json'),true);
			$dbHost = $config['DB_HOST'];
			$dbName = $config['DB_NAME'];
			$dbUser = $config['DB_USER'];
			$dbPass = $config['DB_PASS'];
		}

		if (!self::$instance) {
			self::$instance = new PDO("mysql:host=". $dbHost . ";dbname=" . $dbName, $dbUser, $dbPass);
			self::$instance->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
		}
		return self::$instance;
	}

}

?>