La première section explique comment démarrer le système pour que le client
puisse l'utiliser.  La deuxième section montre comment utiliser le client.

<H1>1. Preparation</H1>

Nous explicons les composantes du système et les commandes utilisées pour
l'utiliser.  Nous recommendons de passer directement à la section 1.3 (TMUX) et
de ne lire les sections 1.1 et 1.2 seulement si on a des problèmes à utiliser
TMUX.

<H2>1.1 Explication</H2>
Le système est composé d'un répartiteur (dispatcher), un service de noms LDAP et
une série de serveurs.

Un client peut lire un fichier contenant une liste d'opéraitons et les envoyer
au répartiteur qui distribue les opérations aux serveurs, récupère les résultats
et les retourne au client.

Le service LDAP fournit une liste de serveurs au répartiteur et une certaine
forme d'authentification.

Tout sauf les serveurs rouleront sur la machine locale et tous les serveurs sauf
un roulerons sur d'autres machines.

<H2>1.2 Commandes</H2>

Nous explicons ici les commandes à exécuter pour initialiser et utiliser le
système.  Notons qu'à la fin, unt procédure avec TMUX automatise tout ce
processus de façon bien plus simple.

Toutes les commandes doivent être exécutées dans des terminaux séparés ou toutes
être mises en background.

Si les commandes sont mises en background, leurs outputs seront mélangés.  Si
les commandes sont exécutées dans des termnaux séparés, ça devient fastidieu.
Dans tous les cas on a beaucoup de commandes à écrire.  La solution avec TMUX
exécute toutes les commandes et affiche leurs outputs de façon conviviale.

<H3>Démarrer les serveurs distant</H3>

Des scripts prepare_distant_n démarrent rmiregistry et un serveur.  Ces scripts
doivent être exécutées sur quelques machines distantes par ssh:

	$ ssh <machine-distante> "cd <emplacement-du-projet> ; ./prepare_distant_n"

<H3>Demarrer les éléments locaux</H3>

Des scripts encapsulent les commandes à exécuter pour démarrer les différentes
parties du système.  Tout doit être exécuté dans le dossier du projet.

Mais avant on doit lancer rimregistry.

	$ CLASSPATH=bin rmireistry &

Ensuite, on doit démarrer un serveur LDAP (c'est la première composante à lancer
car les autre composantes cherchent le service LDAP à leur création):

	$ ./LDAP

Ensuite, on lance le dispatcher:

	$ ./dispatcher

et finalement on lance un serveur sur notre machine locale:

	$ ./server

<H2>1.3 Alternative avec TMUX</H2>

Dans un terminal on lance TMUX:

	$ tmux

Une fois dans tmux, on charge le fichier launch-everything-school.tmux:

	$ tmux source-file launch-everything-new.tmux

La fenêtre sera séparée en plusieurs "panneaux" et toutes les commandes
mentionnées ci-haut seront envoyées à des panneaux différents.

Finalement l'usager sera placé dans un panneau dans lequel il pourra écrire des
commandes selon le mode d'utilisation décrit dans la section suivante.

Pour mettre fin à tous les programmes lancés, la commande

	$ tmux kill-window

arrêtera toutes les commandes lancées.

<H1>2 Utilisation</H1>

Pour utiliser le système, on lance le client et on lui passe, d'une part
l'adresse ip du dipatcher (qui sera toujours 127.0.0.1) et un fichier contenant
une liste d'opérations.

	$ ./client <dispatcher-ip> <operation-file>

Le script test.sh peut aussi être utilisé.  Celui ci ne fait qu'exécuter la
commande précédente avec des arguments fixés au préalable.
